import axios from 'axios'

/**
 * The one HTTP client every view uses.
 *
 * The base URL is read from the build environment instead of being hard-coded to
 * `http://localhost:8081`. A deployment behind a load balancer serves the API on
 * its own origin (or a gateway), and a hard-coded localhost URL cannot reach that
 * from a user's browser. The localhost value is kept as the default so the
 * existing `npm run dev` workflow is unchanged; set `VITE_API_BASE_URL` (see
 * `.env.example`) to point at a different origin.
 *
 * A request timeout is set for the same reason: without one a request against an
 * overloaded or unreachable instance stays open until the browser's own (very
 * long) default, so the UI shows a spinner indefinitely and the connection is
 * held on both ends. 30s is comfortably above the measured p99 of the heaviest
 * endpoint and still bounded.
 */
export const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? 'http://localhost:8081'

export const API_TIMEOUT_MS = Number(import.meta.env?.VITE_API_TIMEOUT_MS ?? 30000)

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT_MS,
})

api.interceptors.request.use(
  (config) => {
    const stored = localStorage.getItem('user_data')
    if (stored) {
      try {
        const user = JSON.parse(atob(stored))
        if (user && user.token) {
          if (!config.headers) {
             config.headers = {}
          }
          if (typeof config.headers.set === 'function') {
            config.headers.set('Authorization', `Bearer ${user.token}`)
          } else {
            config.headers['Authorization'] = `Bearer ${user.token}`
          }
        }
      } catch (e) {
        console.error('Error parsing user data for API auth', e)
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * A 401 on a request that carried a token means the server no longer accepts that
 * token: the account signed out (possibly from another tab or device), its password
 * changed, or it was removed. The stored session is dropped and the page reloads so
 * the router guard sends this tab to /login, instead of leaving a signed-in-looking
 * shell whose every request fails.
 *
 * A refused sign-in is excluded - it also answers 401, but it must be shown to the
 * user as a wrong-credentials message rather than a session expiry.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const config = error?.config
    const headers = config?.headers
    const authorization = headers
      ? (typeof headers.get === 'function' ? headers.get('Authorization') : headers.Authorization)
      : null
    const url = typeof config?.url === 'string' ? config.url : ''
    // Signing in answers 401 for wrong credentials; signing out can answer 401 when
    // the token was already revoked. Neither means "this session just ended".
    const isAuthEndpoint = url.includes('/api/users/login')
      || url.includes('/api/users/google-login')
      || url.includes('/api/users/logout')

    if (status === 401 && authorization && !isAuthEndpoint) {
      localStorage.removeItem('user_data')
      if (typeof window !== 'undefined' && typeof window.location?.reload === 'function') {
        window.location.reload()
      }
    }

    return Promise.reject(error)
  }
)

export default api

