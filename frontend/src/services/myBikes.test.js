import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('./apiClient.js', () => ({
  requestApi: vi.fn(),
}))

import { requestApi } from './apiClient.js'
import { getBikeDetail, listMyBikes, toKinematicsData } from './myBikes.js'

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
