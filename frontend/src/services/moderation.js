import { requestApi } from './apiClient.js'

export function listPendingBikes() {
  return requestApi('/api/moderation/pending')
}

export function approveBike(bikeId) {
  return requestApi(`/api/moderation/${bikeId}/approve`, { method: 'POST' })
}

export function rejectBike(bikeId) {
  return requestApi(`/api/moderation/${bikeId}/reject`, { method: 'POST' })
}

export function removeBike(bikeId, reason) {
  return requestApi(`/api/moderation/${bikeId}/remove`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
}
