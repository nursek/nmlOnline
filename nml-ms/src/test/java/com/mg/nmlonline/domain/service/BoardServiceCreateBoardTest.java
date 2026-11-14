package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService — création explicite")
class BoardServiceCreateBoardTest {

    @Mock
    BoardRepository boardRepository;
    @Mock
    BoardMapper boardMapper;
    @Mock
    PlayerRepository playerRepository;
    @Mock
    AllianceGraph allianceGraph;
    @InjectMocks
    BoardService boardService;

    @Test
    @DisplayName("Une création avec un nom existant est refusée")
    void refusesDuplicateName() {
        BoardDto dto = new BoardDto();
        dto.setName("Carte Principale");
        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(new Board()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> boardService.createBoardDto(dto));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
