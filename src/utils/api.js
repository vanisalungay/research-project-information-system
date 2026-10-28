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

export default api

