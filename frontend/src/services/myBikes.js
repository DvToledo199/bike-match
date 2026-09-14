import { requestApi } from './apiClient.js'

export function listMyBikes() {
  return requestApi('/api/my-bikes')
}

export function getBikeDetail(bikeId) {
  return requestApi(`/api/bikes/${bikeId}`)
}

export function toKinematicsData(bike) {
  if (!bike?.result) return null

  return {
    ...bike.result.curves,
    ...bike.result.descriptors,
  }
}

export function publishBike(bikeId) {
  return requestApi(`/api/bikes/${bikeId}/publish`, { method: 'POST' })
}
