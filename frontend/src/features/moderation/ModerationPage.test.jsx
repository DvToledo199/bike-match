import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import ModerationPage from './ModerationPage.jsx'
import { approveBike, listPendingBikes, rejectBike, removeBike } from '../../services/moderation.js'
import { listPublicBikes } from '../../services/myBikes.js'

vi.mock('../../services/moderation.js', () => ({
  approveBike: vi.fn(), listPendingBikes: vi.fn(), rejectBike: vi.fn(), removeBike: vi.fn(),
}))
vi.mock('../../services/myBikes.js', () => ({ listPublicBikes: vi.fn() }))

const session = { role: 'MODERATOR', accessToken: 'token' }
const bike = {
  id: 7, brand: 'Orange', model: 'Stage 6', modelYear: 2020, category: 'ENDURO',
  photoUrl: 'https://example.com/bike.jpg', ownerUsername: 'david', requestedAt: '2026-09-20T12:00:00Z',
}
const publicBike = {
  id: 8, brand: 'Specialized', model: 'Enduro', modelYear: 2023, category: 'ENDURO',
  photoUrl: null, createdAt: '2026-09-18T12:00:00Z',
}

beforeEach(() => {
  vi.resetAllMocks()
  listPendingBikes.mockResolvedValue([bike])
  listPublicBikes.mockResolvedValue({ items: [publicBike], page: 0, totalPages: 1, hasNext: false })
  approveBike.mockResolvedValue({ id: 7, status: 'PUBLIC' })
  rejectBike.mockResolvedValue({ id: 7, status: 'REJECTED' })
  removeBike.mockResolvedValue(null)
})

it('loads the unpaginated queue, including the owner and request date', async () => {
  render(<ModerationPage session={session} />)
  expect(screen.getByText('Loading bikes…')).toBeTruthy()
  expect(await screen.findByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
  expect(screen.getByText('Submitted by @david')).toBeTruthy()
  expect(screen.getByText('Requested on', { exact: false }).querySelector('time').dateTime).toBe(bike.requestedAt)
  expect(listPendingBikes).toHaveBeenCalledWith()
  expect(listPublicBikes).not.toHaveBeenCalled()
  expect(screen.queryByRole('navigation', { name: 'Catalog pages' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Next' })).toBeNull()
})

it('shows an empty queue and lets an administrator moderate too', async () => {
  listPendingBikes.mockResolvedValue([])
  render(<ModerationPage session={{ ...session, role: 'ADMIN' }} />)
  expect(await screen.findByText('No bikes are waiting for review.')).toBeTruthy()
})

it.each([null, { role: 'USER', accessToken: 'token' }])('does not fetch privileged data for %s', async (account) => {
  render(<ModerationPage session={account} />)
  expect(screen.getByRole('alert').textContent).toContain(account ? 'does not have permission' : 'Log in')
  expect(listPendingBikes).not.toHaveBeenCalled()
  expect(listPublicBikes).not.toHaveBeenCalled()
})

it.each([
  [400, 'Check the information'], [401, 'Log in'], [403, 'does not have permission'],
  [404, 'no longer exists'], [409, 'already reviewed'], [500, 'could not be confirmed'],
])('explains a loading HTTP %s without showing raw server details', async (status, message) => {
  listPendingBikes.mockRejectedValue({ status, detail: 'Internal database detail' })
  render(<ModerationPage session={session} />)
  expect((await screen.findByRole('alert')).textContent).toContain(message)
  expect(screen.queryByText('Internal database detail')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Approve' })).toBeNull()
})

it('recovers from a connection error when retrying the load', async () => {
  listPendingBikes.mockRejectedValueOnce({ kind: 'network' }).mockResolvedValueOnce([])
  render(<ModerationPage session={session} />)
  expect((await screen.findByRole('alert')).textContent).toContain('Check your connection')
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByText('No bikes are waiting for review.')).toBeTruthy()
})

it.each([
  ['Approve', approveBike, 'Approving…', 'Orange Stage 6 was approved and is now public.'],
  ['Reject', rejectBike, 'Rejecting…', 'Orange Stage 6 was rejected and remains outside the public catalog.'],
])('waits for the server before completing %s and prevents duplicate submissions', async (label, service, busyLabel, success) => {
  let complete
  service.mockReturnValue(new Promise((resolve) => { complete = resolve }))
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: label }))
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
  expect(screen.queryByText(success)).toBeNull()
  const busyButton = screen.getByRole('button', { name: busyLabel })
  expect(busyButton.disabled).toBe(true)
  fireEvent.click(busyButton)
  expect(service).toHaveBeenCalledExactlyOnceWith(7)
  expect(screen.getByRole('button', { name: 'Published' }).disabled).toBe(true)
  listPendingBikes.mockResolvedValue([])
  await act(async () => { complete({ id: 7 }) })
  expect(await screen.findByText(success)).toBeTruthy()
  expect(await screen.findByText('No bikes are waiting for review.')).toBeTruthy()
})

it.each([404, 409])('keeps an unconfirmed bike on action HTTP %s and requires a refresh', async (status) => {
  approveBike.mockRejectedValue({ status })
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Approve' }))
  expect((await screen.findByRole('alert')).textContent).toContain('Refresh the list')
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Approve' }).disabled).toBe(true)
  expect(screen.queryByText(/was approved/)).toBeNull()
  listPendingBikes.mockResolvedValue([])
  fireEvent.click(screen.getByRole('button', { name: 'Refresh list' }))
  expect(await screen.findByText('No bikes are waiting for review.')).toBeTruthy()
  expect(screen.queryByRole('alert')).toBeNull()
})

it.each([401, 403])('handles permission loss during a decision (%s)', async (status) => {
  rejectBike.mockRejectedValue({ status })
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Reject' }))
  expect((await screen.findByRole('alert')).textContent).toContain(status === 401 ? 'Log in' : 'does not have permission')
  expect(screen.queryByRole('heading', { name: 'Orange Stage 6' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Reject' })).toBeNull()
})

it('requires a removal reason, retains it after HTTP 400 and waits for 204', async () => {
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Remove bike' }))
  const input = screen.getByRole('textbox', { name: 'Reason for removal' })
  expect(input.maxLength).toBe(500)
  expect(screen.getByText(/This permanently deletes/)).toBeTruthy()
  fireEvent.change(input, { target: { value: '   ' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm permanent removal' }))
  expect(screen.getByRole('alert').textContent).toContain('not just spaces')
  expect(removeBike).not.toHaveBeenCalled()

  removeBike.mockRejectedValueOnce({ status: 400 })
  fireEvent.change(input, { target: { value: '  Photo copied without permission.  ' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm permanent removal' }))
  expect((await screen.findByRole('alert')).textContent).toContain('Check the information')
  expect(input.value).toBe('  Photo copied without permission.  ')
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()

  let complete
  removeBike.mockReturnValueOnce(new Promise((resolve) => { complete = resolve }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm permanent removal' }))
  expect(screen.getByRole('button', { name: 'Removing…' }).disabled).toBe(true)
  expect(screen.queryByText(/was permanently removed/)).toBeNull()
  listPendingBikes.mockResolvedValue([])
  await act(async () => { complete(null) })
  expect(await screen.findByText(/Orange Stage 6 was permanently removed/)).toBeTruthy()
  expect(removeBike).toHaveBeenLastCalledWith(7, 'Photo copied without permission.')
})

it('can cancel removal without sending anything', async () => {
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Remove bike' }))
  fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(screen.queryByRole('textbox')).toBeNull()
  expect(removeBike).not.toHaveBeenCalled()
})

it('rejects overlong reasons and accepts exactly 500 characters', async () => {
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Remove bike' }))
  const input = screen.getByRole('textbox', { name: 'Reason for removal' })
  expect(document.activeElement).toBe(input)
  fireEvent.change(input, { target: { value: 'a'.repeat(501) } })
  fireEvent.submit(screen.getByRole('form', { name: 'Remove Orange Stage 6' }))
  expect(removeBike).not.toHaveBeenCalled()
  expect(screen.getByRole('alert').textContent).toContain('500 characters')
  fireEvent.change(input, { target: { value: 'a'.repeat(500) } })
  listPendingBikes.mockResolvedValue([])
  fireEvent.click(screen.getByRole('button', { name: 'Confirm permanent removal' }))
  expect(await screen.findByText(/Orange Stage 6 was permanently removed/)).toBeTruthy()
  expect(removeBike).toHaveBeenCalledExactlyOnceWith(7, 'a'.repeat(500))
})

it.each([{ kind: 'network' }, { kind: 'timeout' }, { status: 500 }])('does not assume success on an uncertain response (%s)', async (error) => {
  approveBike.mockRejectedValue(error)
  render(<ModerationPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Approve' }))
  await screen.findByRole('alert')
  expect(screen.queryByText(/was approved/)).toBeNull()
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Approve' }).disabled).toBe(true)
  expect(screen.getByRole('button', { name: 'Refresh list' }).disabled).toBe(false)
})

it('replaces a failed photo with the existing placeholder', async () => {
  render(<ModerationPage session={session} />)
  fireEvent.error(await screen.findByRole('img', { name: 'Photo of Orange Stage 6' }))
  expect(screen.getByText('No photo')).toBeTruthy()
})

it('can retry a failed published catalog load', async () => {
  listPublicBikes.mockRejectedValueOnce({ status: 500 }).mockResolvedValueOnce({ items: [], page: 0, totalPages: 0, hasNext: false })
  render(<ModerationPage session={session} />)
  fireEvent.click(screen.getByRole('button', { name: 'Published' }))
  await screen.findByRole('alert')
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByText('There are no published bikes.')).toBeTruthy()
  expect(listPublicBikes).toHaveBeenLastCalledWith({ page: 0 })
})

it('paginates only published bikes and never displays an owner there', async () => {
  listPublicBikes.mockResolvedValueOnce({ items: [publicBike], page: 0, totalPages: 2, hasNext: true })
    .mockResolvedValueOnce({ items: [{ ...publicBike, id: 9, model: 'Stumpjumper' }], page: 1, totalPages: 2, hasNext: false })
  render(<ModerationPage session={session} />)
  await screen.findByText('Submitted by @david')
  fireEvent.click(screen.getByRole('button', { name: 'Published' }))
  expect(await screen.findByRole('heading', { name: 'Specialized Enduro' })).toBeTruthy()
  expect(screen.queryByText(/Submitted by/)).toBeNull()
  expect(screen.queryByRole('button', { name: 'Approve' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Reject' })).toBeNull()
  expect(screen.getByRole('button', { name: 'Previous' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))
  expect(await screen.findByRole('heading', { name: 'Specialized Stumpjumper' })).toBeTruthy()
  expect(listPublicBikes).toHaveBeenLastCalledWith({ page: 1 })
  expect(screen.getByRole('button', { name: 'Next' }).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Pending' }))
  expect(await screen.findByText('Submitted by @david')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Next' })).toBeNull()
})

it('moves back to an existing catalog page after removing the last bike on a page', async () => {
  listPublicBikes.mockResolvedValueOnce({ items: [publicBike], page: 0, totalPages: 2, hasNext: true })
    .mockResolvedValueOnce({ items: [publicBike], page: 1, totalPages: 2, hasNext: false })
    .mockResolvedValueOnce({ items: [], page: 1, totalPages: 1, hasNext: false })
    .mockResolvedValueOnce({ items: [], page: 0, totalPages: 0, hasNext: false })
  render(<ModerationPage session={session} />)
  fireEvent.click(screen.getByRole('button', { name: 'Published' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Next' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Next' }).disabled).toBe(true))
  fireEvent.click(screen.getByRole('button', { name: 'Remove bike' }))
  fireEvent.change(screen.getByRole('textbox', { name: 'Reason for removal' }), { target: { value: 'Spam.' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm permanent removal' }))
  expect(await screen.findByText('There are no published bikes.')).toBeTruthy()
  expect(listPublicBikes).toHaveBeenLastCalledWith({ page: 0 })
  expect(removeBike).toHaveBeenCalledExactlyOnceWith(8, 'Spam.')
})

it('ignores a late pending response after switching views', async () => {
  let complete
  listPendingBikes.mockReturnValue(new Promise((resolve) => { complete = resolve }))
  render(<ModerationPage session={session} />)
  fireEvent.click(screen.getByRole('button', { name: 'Published' }))
  await screen.findByRole('heading', { name: 'Specialized Enduro' })
  await act(async () => { complete([bike]) })
  expect(screen.queryByRole('heading', { name: 'Orange Stage 6' })).toBeNull()
  expect(screen.getByRole('heading', { name: 'Specialized Enduro' })).toBeTruthy()
})
