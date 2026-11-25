# Security

Security model of NML Online, what an operator must do, and what is knowingly left open.
The system design is in [ARCHITECTURE.md](ARCHITECTURE.md).

## 1. Reporting a vulnerability

Report privately to the repository owner through GitHub (Security → Report a vulnerability, or a
direct message). Do not open a public issue for an exploitable defect.

## 2. Authentication

| Element | Behaviour |
|---|---|
| Passwords | BCrypt over the password prefixed with a server-side pepper (`JWT_PEPPER`) |
| Access token | HMAC-signed JWT, claim `type=access`, 10 minutes, kept in `sessionStorage` |
| Refresh token | JWT, claim `type=refresh`, 1 day (30 days with "remember me"), cookie `HttpOnly`, `Secure`, `SameSite=Strict`, path `/api/auth` |
| Rotation | Every refresh issues a new refresh token; the server stores only a hash; replaying a rotated token revokes the session |
| Type separation | An access token is never accepted as a refresh token, nor the reverse |
| Unknown user | Login performs the same number of BCrypt computations whether or not the account exists |

The access token is readable by JavaScript by design; the mitigations are its short life, the
strict Content-Security-Policy and the fact that the long-lived credential is the `HttpOnly` cookie.

`JwtSecretValidator` refuses to start with a secret shorter than 32 characters, a pepper shorter
than 16, or a value previously published in this repository.

## 3. Authorisation

- The caller is always taken from the token (`userId` request attribute). No endpoint trusts a
  `playerId`, an owner or a price sent by the client.
- Every game endpoint is scoped by `X-Board-Id`. `BoardScopeFilter` rejects a caller who is neither
  platform admin, game master nor player of that game (also when the header is omitted on a
  single-game server), and services resolve the player by
  `(userId, boardId)`.
- `/api/admin/**` requires `ADMIN` or the per-game `INSTANCE_ADMIN`. Platform operations carry
  `@PreAuthorize("hasRole('ADMIN')")` on the method.
- A game master is confined to the players of their own game, including when the path is
  percent-encoded: all filters compare the decoded path, which is the form Spring MVC routes on.
- A player's private state (money, inventory, orders) is returned only to its owner and to
  administrators; the map uses a public projection.
- Gazette drafts and editor notes are administrator-only.

## 4. Abuse protection

- Login, registration and refresh: 5 attempts, then a one-minute block, per account and per IP.
- Authenticated mutations: 120 per minute per account.
- Page size is capped at 500; uploads at 10 MB per file.
- Counters are in memory: they reset on restart and are not shared between instances.

Behind a reverse proxy the client IP is read from `X-Forwarded-For`
(`server.forward-headers-strategy=native`). The proxy must be the only way to reach the container,
otherwise that header can be forged to evade the per-IP limit.

## 5. Browser-side protections

- Content-Security-Policy restricted to `'self'` for scripts, no framing, HSTS with subdomains,
  `Referrer-Policy: no-referrer`.
- CSRF protection is disabled for `/api/**` only: the API authenticates with a bearer header, and
  the refresh cookie is `SameSite=Strict`.
- CORS uses an explicit origin list (`CorsConfig`, extended by `APP_CORS_ALLOWED_ORIGINS`).
- Uploaded board images are validated by magic bytes. Uploaded SVG overlays are filtered on the
  server, served with a `sandbox` CSP under `/boards/**`, and sanitised again by the client before
  being inlined.

## 6. Operator checklist

1. Provide `JWT_SECRET`, `JWT_PEPPER`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` and
   `ADMIN_PASSWORD` through the environment or a secret store. None has a default in production.
2. Generate secrets randomly. The role is read from the token, so a known signing key allows
   forging an administrator token. No valid key is committed: tests generate theirs at run time
   (`TestSecrets`) and every value that was once committed is rejected at startup.
3. Keep `JWT_PEPPER` stable. Changing it invalidates every stored password.
4. Run the image with its default profile (`prod`). Any other profile uses an in-memory database
   and opens registration.
5. Leave `REGISTRATION_INVITE_CODE` empty to close registration, or set it to allow
   invitation-only sign-up. Treat the code as a secret.
6. Terminate TLS in front of the container and do not expose port 8080 directly.
7. Mount volumes on `/app/static/boards` and `/app/logs`.
8. Demo mode (`APP_IMPORT_DEMO_DATA=true`) creates one account per fixture player, with the
   username as password. Run it on a disposable database and delete these accounts before a real
   game — see below.

## 7. Known limitations

| Limitation | Risk | Mitigation today |
|---|---|---|
| Demo accounts outlive demo mode: the demo reset keeps accounts | After demo mode is switched off, anyone can still log in to them with `username/username` | Delete or rename these accounts before opening a real game; never point demo mode at the real database |
| Rate limiters and turn locks are in memory | Lost on restart; ineffective across several instances | Single-instance deployment |
| Access token in `sessionStorage` | Readable by a successful XSS for up to 10 minutes | Strict CSP, no `innerHTML` outside the sanitised map overlay |
| Secrets committed in history | Old values are public | Rejected at startup by `JwtSecretValidator` |

## 8. Dependencies

`npm audit --omit=dev` reports no known vulnerability at the time of writing. Dependabot alerts and
security updates are enabled, so a pull request appears only when a published vulnerability affects a
dependency in use; version bumps are applied manually, when a release is worth taking.
