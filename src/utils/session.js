/**
 * Session helpers.
 *
 * The authenticated user is persisted (btoa-encoded JSON) under `user_data` by the
 * Pinia user store. These helpers let data-fetching pages verify which account a
 * response belongs to so a previous account's records can never linger on screen
 * after switching users.
 */

/** Id of the currently authenticated user, or null when not logged in. */
export function currentUserId() {
  const stored = localStorage.getItem('user_data')
  if (!stored) return null
  try {
    return JSON.parse(atob(stored))?.id ?? null
  } catch {
    return null
  }
}
