import { requestApi } from './apiClient.js'

export function listUsers() {
  return requestApi('/api/admin/users')
}

export function changeUserRole(userId, role) {
  return requestApi(`/api/admin/users/${userId}/role`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  })
}
