/**
 * Constantes applicatives partagées.
 * Centralise les timeouts, durées de SnackBar, cooldowns et seuils.
 */

export const APP_CONSTANTS = {
  // HTTP
  HTTP_TIMEOUT_MS: 15_000,
  HTTP_RETRY_COUNT: 1,
  HTTP_RETRY_DELAY_MS: 500,

  // Auth
  REFRESH_COOLDOWN_MS: 3_000,

  // UI feedback
  SNACKBAR_SHORT_DURATION_MS: 3_000,
} as const;
