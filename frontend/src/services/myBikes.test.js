import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('./apiClient.js', () => ({
  requestApi: vi.fn(),
}))

import { requestApi } from './apiClient.js'
import { listMyBikes } from './myBikes.js'

beforeEach(() => {
  vi.clearAllMocks()
})

it('requests the authenticated user bike summaries', async () => {
  requestApi.mockResolvedValue([])

  await listMyBikes()

  expect(requestApi).toHaveBeenCalledWith('/api/my-bikes')
})
