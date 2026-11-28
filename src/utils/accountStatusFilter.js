/**
 * accountStatusFilter.js
 * ---------------------------------------------------------------------------
 * The status filter of the administrator's User Accounts page.
 *
 * The filter is client-side: GET /api/users returns the whole account list and the
 * page already pages and searches it locally, so a status filter belongs in the same
 * pass rather than in a second request. That keeps the search, the status filter and
 * the pagination working on ONE result set - the page counter always describes the
 * rows the selected filter actually produced.
 *
 * The values are the account statuses the backend really stores
 * (UserService: PENDING, APPROVED, REJECTED, INACTIVE). The wording and the colour
 * of each one come from the shared status catalog, never from this file.
 *
 * `REVOKED` is deliberately NOT an account status here: it is the state of a Dean
 * *appointment* (DeanAssignment), not of the account, which stays APPROVED. A filter
 * for it would have to invent an account status the system does not have.
 *
 * Verifiable without a browser - see accountStatusFilter.test.js.
 */
import {
  ACCOUNT_STATUS_LABELS,
  accountStatusLabel,
  genericStatusLabel,
  normalizeStatusKey,
} from './statusCatalog.js'

/** The stored account statuses, in the order the filter offers them. */
export const ACCOUNT_STATUS_FILTER_ORDER = Object.freeze(
  Object.keys(ACCOUNT_STATUS_LABELS),
)

/** The comparison key of an account status (`approved`, `Approved` -> `APPROVED`). */
export function normalizeAccountStatus(value) {
  return normalizeStatusKey(value)
}

/**
 * Whether one account belongs to the selected status. An empty selection is the
 * "All Accounts" option and matches every row.
 */
export function matchesAccountStatus(user, status) {
  const wanted = normalizeAccountStatus(status)
  if (!wanted) return true
  return normalizeAccountStatus(user?.status) === wanted
}

/**
 * The filter options, with the real number of accounts in each. Every status the
 * backend supports is offered even when no account currently holds it (so an empty
 * queue is visible as empty), and a status found in the data but not in the catalog
 * is appended rather than hidden. No status is invented.
 *
 * @param {Array<object>} users the loaded account list
 * @returns {Array<{value: string, label: string, count: number}>}
 */
export function accountStatusFilterOptions(users) {
  const list = Array.isArray(users) ? users : []

  const counts = new Map()
  for (const user of list) {
    const key = normalizeAccountStatus(user?.status)
    if (!key) continue
    counts.set(key, (counts.get(key) || 0) + 1)
  }

  const values = ACCOUNT_STATUS_FILTER_ORDER.map(normalizeAccountStatus)
  for (const key of [...counts.keys()].sort()) {
    if (!values.includes(key)) values.push(key)
  }

  return values.map((value) => ({
    value,
    label: accountStatusLabel(value) || genericStatusLabel(value),
    count: counts.get(value) || 0,
  }))
}
