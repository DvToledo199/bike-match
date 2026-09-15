import { requestApi } from './apiClient.js'

export function listMyBikes() {
  return requestApi('/api/my-bikes')
}

export function listPublicBikes({ category = '', page = 0 } = {}) {
  const params = new URLSearchParams({ page: String(page) })
  if (category) params.set('category', category)
  return requestApi(`/api/bikes?${params.toString()}`)
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

export function deleteBike(bikeId) {
  return requestApi(`/api/bikes/${bikeId}`, { method: 'DELETE' })
}

export function listMyNotices() {
  return requestApi('/api/my-notices')
}

export function dismissNotice(noticeId) {
  return requestApi(`/api/my-notices/${noticeId}/dismiss`, { method: 'POST' })
}
