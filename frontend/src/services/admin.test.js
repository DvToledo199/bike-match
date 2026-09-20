import { beforeEach, expect, it, vi } from 'vitest'
import { requestApi } from './apiClient.js'
import { changeUserRole, listUsers } from './admin.js'

vi.mock('./apiClient.js', () => ({ requestApi: vi.fn() }))

beforeEach(() => { vi.resetAllMocks() })

it('requests the account summaries without pagination or private profile fields', async () => {
  requestApi.mockResolvedValue([{ id: 2, username: 'rider', role: 'USER' }])
  expect(await listUsers()).toEqual([{ id: 2, username: 'rider', role: 'USER' }])
  expect(requestApi).toHaveBeenCalledWith('/api/admin/users')
})

it.each(['USER', 'MODERATOR'])('changes an account to %s and accepts an empty response', async (role) => {
  requestApi.mockResolvedValue(null)
  expect(await changeUserRole(2, role)).toBeNull()
  expect(requestApi).toHaveBeenCalledWith('/api/admin/users/2/role', {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role }),
  })
})

it('preserves self-change conflicts for the page to explain', async () => {
  const error = { status: 409 }
  requestApi.mockRejectedValue(error)
  await expect(changeUserRole(1, 'USER')).rejects.toBe(error)
})
