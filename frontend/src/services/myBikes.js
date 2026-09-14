import { requestApi } from './apiClient.js'

export function listMyBikes() {
  return requestApi('/api/my-bikes')
}
