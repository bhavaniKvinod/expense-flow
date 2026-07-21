import axios from 'axios'

const TOKEN_KEY = 'expenseflow.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

const client = axios.create({ baseURL: '/api' })

// Attach the JWT to every request.
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On 401, drop the token so the app falls back to the login screen.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      setToken(null)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

// Normalises an axios error into a user-facing message, preserving policy violations.
export function errorMessage(error) {
  const data = error.response?.data
  if (!data) return error.message || 'Something went wrong'
  if (data.violations?.length) {
    return data.violations.map((v) => v.message).join(' ')
  }
  if (data.fieldErrors) {
    return Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join(' ')
  }
  return data.message || 'Request failed'
}

export default client
