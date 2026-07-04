import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081',
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
