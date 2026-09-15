import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('./apiClient.js', () => ({
  requestApi: vi.fn(),
}))

import { requestApi } from './apiClient.js'
import {
  deleteBike,
  dismissNotice,
  getBikeDetail,
  listMyBikes,
  listMyNotices,
  listPublicBikes,
  toKinematicsData,
} from './myBikes.js'

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

it('requests the notices about bikes removed by moderation', async () => {
  requestApi.mockResolvedValue([])

  await listMyNotices()

  expect(requestApi).toHaveBeenCalledWith('/api/my-notices')
})

it('dismisses one notice', async () => {
  requestApi.mockResolvedValue(null)

  await dismissNotice(3)

  expect(requestApi).toHaveBeenCalledWith('/api/my-notices/3/dismiss', { method: 'POST' })
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
