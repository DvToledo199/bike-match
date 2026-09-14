import { requestApi } from './apiClient.js'

export function loginUser(credentials) {
  return requestApi('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  })
}
