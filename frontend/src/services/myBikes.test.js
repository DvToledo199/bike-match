import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('./apiClient.js', () => ({
  requestApi: vi.fn(),
}))

import { requestApi } from './apiClient.js'
import { deleteBike, getBikeDetail, listMyBikes, listPublicBikes, toKinematicsData } from './myBikes.js'

beforeEach(() => {
  vi.clearAllMocks()
})

it('requests the authenticated user bike summaries', async () => {
  requestApi.mockResolvedValue([])

  await listMyBikes()

  expect(requestApi).toHaveBeenCalledWith('/api/my-bikes')
})

it('requests one bike detail', async () => {
  requestApi.mockResolvedValue({ id: 7 })

  await getBikeDetail(7)

  expect(requestApi).toHaveBeenCalledWith('/api/bikes/7')
})

it('deletes one bike', async () => {
  requestApi.mockResolvedValue(null)

  await deleteBike(7)

  expect(requestApi).toHaveBeenCalledWith('/api/bikes/7', { method: 'DELETE' })
})

it('requests a public catalog page and optional category', async () => {
  requestApi.mockResolvedValue({ items: [] })

  await listPublicBikes({ category: 'ENDURO', page: 2 })

  expect(requestApi).toHaveBeenCalledWith('/api/bikes?page=2&category=ENDURO')
})

it('adapts persisted curves and descriptors for the existing charts', () => {
  const bike = {
    result: {
      curves: { leverageCurve: [{ ratio: 2.5 }] },
      descriptors: { travelCheck: { withinTolerance: true } },
    },
  }

  expect(toKinematicsData(bike)).toEqual({
    leverageCurve: [{ ratio: 2.5 }],
    travelCheck: { withinTolerance: true },
  })
})
