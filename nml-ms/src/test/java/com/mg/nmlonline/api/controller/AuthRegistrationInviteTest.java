package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.domain.model.user.RegisterRequest;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.domain.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRegistrationInviteTest {

    private final UserService userService = mock(UserService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AuthController controller(boolean open, String inviteCode) {
        return new AuthController(userService, mock(JwtService.class), passwordEncoder,
                "pepper", true, open, inviteCode);
    }

    private RegisterRequest request(String inviteCode) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("invite-user");
        request.setPassword("password123");
        request.setInviteCode(inviteCode);
        return request;
    }

    @Test
    void refusesRegistrationWhenInviteIsMissingOrWrong() {
        AuthController controller = controller(false, "secret");
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> controller.register(request(null), new MockHttpServletRequest()));
        assertEquals(HttpStatus.FORBIDDEN, missing.getStatusCode());

        ResponseStatusException wrong = assertThrows(ResponseStatusException.class,
                () -> controller.register(request("nope"), new MockHttpServletRequest()));
        assertEquals(HttpStatus.FORBIDDEN, wrong.getStatusCode());
    }

    @Test
    void refusesRegistrationWhenInviteIsDisabled() {
        ResponseStatusException disabled = assertThrows(ResponseStatusException.class,
                () -> controller(false, "").register(request(null), new MockHttpServletRequest()));
        assertEquals(HttpStatus.FORBIDDEN, disabled.getStatusCode());
    }

    @Test
    void acceptsRegistrationWithInviteCode() {
        when(userService.findByUsername("invite-user")).thenReturn(null);
        when(userService.encodePassword("password123")).thenReturn("hash");

        assertEquals(200, controller(false, "secret")
                .register(request("secret"), new MockHttpServletRequest()).getStatusCode().value());
        verify(userService).save(any());
    }

    @Test
    void staysOpenInDevWhenConfiguredOpen() {
        when(userService.findByUsername("invite-user")).thenReturn(null);
        when(userService.encodePassword("password123")).thenReturn("hash");

        assertEquals(200, controller(true, "")
                .register(request(null), new MockHttpServletRequest()).getStatusCode().value());
    }
}
