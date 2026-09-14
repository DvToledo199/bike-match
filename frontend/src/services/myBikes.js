import { requestApi } from './apiClient.js'

export function listMyBikes() {
  return requestApi('/api/my-bikes')
}

export function publishBike(bikeId) {
  return requestApi(`/api/bikes/${bikeId}/publish`, { method: 'POST' })
}
