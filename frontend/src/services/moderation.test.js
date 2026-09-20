import { beforeEach, expect, it, vi } from 'vitest'
import { requestApi } from './apiClient.js'
import { approveBike, listPendingBikes, rejectBike, removeBike } from './moderation.js'

vi.mock('./apiClient.js', () => ({ requestApi: vi.fn() }))

beforeEach(() => { vi.clearAllMocks() })

it('requests the complete pending list without pagination', async () => {
  requestApi.mockResolvedValue([{ id: 7 }])
  expect(await listPendingBikes()).toEqual([{ id: 7 }])
  expect(requestApi).toHaveBeenCalledWith('/api/moderation/pending')
})

it.each([[approveBike, 'approve'], [rejectBike, 'reject']])('sends a %s decision', async (decide, action) => {
  requestApi.mockResolvedValue({ id: 7, status: 'PUBLIC' })
  await decide(7)
  expect(requestApi).toHaveBeenCalledWith(`/api/moderation/7/${action}`, { method: 'POST' })
})

it('sends the removal reason and accepts an empty 204 response', async () => {
  requestApi.mockResolvedValue(null)
  expect(await removeBike(7, 'Photo copied without permission.')).toBeNull()
  expect(requestApi).toHaveBeenCalledWith('/api/moderation/7/remove', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason: 'Photo copied without permission.' }),
  })
})

it('preserves HTTP failures for the page to explain', async () => {
  const error = { status: 409 }
  requestApi.mockRejectedValue(error)
  await expect(approveBike(7)).rejects.toBe(error)
})
