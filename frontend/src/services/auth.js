import { requestApi } from './apiClient.js'

export function registerUser(credentials) {
  return requestApi('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  })
}
