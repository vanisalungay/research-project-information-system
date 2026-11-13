/**
 * roleDashboard.js
 * ---------------------------------------------------------------------------
 * The dashboard each role lands on once it is fully signed in.
 *
 * The login page and the change-password page both need this, and a user who has just
 * changed a temporary password must land in exactly the same place as a user who logged in
 * normally - so the mapping lives in one place instead of being repeated per page.
 *
 * Verifiable without a browser - see roleDashboard.test.js.
 */
export const ROLE_DASHBOARDS = {
  PROPONENT: '/proponent-dashboard',
  RPS_ADMIN: '/rpsadmin-dash',
  RPS_STAFF: '/rpsstaff-dash',
  DEAN: '/dean-dashboard',
  OVCRIGE: '/home',
  OVCAF: '/ovcaf-dash',
  REC: '/rec-dash',
  OC: '/oc-dashboard',
}

/** The dashboard path for a role, or the shared home when the role is unknown. */
export function dashboardForRole(role) {
  return ROLE_DASHBOARDS[role] || '/home'
}