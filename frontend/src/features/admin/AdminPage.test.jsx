import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import AdminPage from './AdminPage.jsx'
import { changeUserRole, listUsers } from '../../services/admin.js'

vi.mock('../../services/admin.js', () => ({ changeUserRole: vi.fn(), listUsers: vi.fn() }))

const session = { username: 'owner', role: 'ADMIN', accessToken: 'token' }
const users = [
  { id: 1, username: 'owner', email: 'owner@example.com', role: 'ADMIN', createdAt: '2026-09-18T09:00:00Z' },
  { id: 2, username: 'rider', email: 'rider@example.com', role: 'USER', createdAt: '2026-09-19T10:00:00Z' },
  { id: 3, username: 'reviewer', email: 'reviewer@example.com', role: 'MODERATOR', createdAt: '2026-09-20T11:00:00Z' },
  { id: 4, username: 'other_admin', email: 'other-admin@example.com', role: 'ADMIN', createdAt: '2026-09-20T12:00:00Z' },
]

beforeEach(() => {
  vi.resetAllMocks()
  listUsers.mockResolvedValue(users)
  changeUserRole.mockResolvedValue(null)
})

it('loads account emails next to their usernames and lists current roles', async () => {
  render(<AdminPage session={session} />)
  expect(screen.getByText('Loading accounts…')).toBeTruthy()
  expect(await screen.findByRole('table', { name: 'User accounts' })).toBeTruthy()
  expect(screen.getAllByRole('columnheader').map((cell) => cell.textContent)).toEqual(['Username / email', 'Role', 'Joined', 'Actions'])
  expect(within(screen.getByRole('row', { name: /rider/ })).getByRole('cell', { name: 'User', exact: true })).toBeTruthy()
  for (const user of users) {
    const identity = screen.getByRole('rowheader', { name: `${user.username} ${user.email}` })
    expect(within(identity).getByText(user.email)).toBeTruthy()
  }
  expect(listUsers).toHaveBeenCalledWith()
})

it('distinguishes similar usernames by email and changes the selected account', async () => {
  listUsers.mockResolvedValue([
    users[1],
    { ...users[1], id: 5, username: 'rider_2', email: 'second@example.com' },
  ])
  render(<AdminPage session={session} />)
  const row = await screen.findByRole('row', { name: /rider_2 second@example.com/ })
  expect(within(row).getByText('second@example.com')).toBeTruthy()
  fireEvent.click(within(row).getByRole('button', { name: 'Change role for @rider_2' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MODERATOR' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm role change' }))
  await screen.findByText(/@rider_2 now has the Moderator role/)
  expect(changeUserRole).toHaveBeenCalledExactlyOnceWith(5, 'MODERATOR')
  expect(within(screen.getByRole('row', { name: /rider rider@example.com/ })).getByRole('cell', { name: 'User', exact: true })).toBeTruthy()
})

it('shows the empty state', async () => {
  listUsers.mockResolvedValue([])
  render(<AdminPage session={session} />)
  expect(await screen.findByText('There are no accounts to display.')).toBeTruthy()
  expect(screen.queryByRole('table')).toBeNull()
})

it.each([null, { role: 'USER' }, { role: 'MODERATOR' }])('does not request accounts for a forced route without admin permission (%s)', (account) => {
  render(<AdminPage session={account} />)
  expect(screen.getByRole('alert').textContent).toContain(account ? 'does not have permission' : 'Log in')
  expect(listUsers).not.toHaveBeenCalled()
  expect(screen.queryByRole('table')).toBeNull()
  expect(screen.queryByText('rider@example.com')).toBeNull()
})

it.each([
  [400, 'Check the selected role'], [401, 'Log in'], [403, 'does not have permission'],
  [404, 'no longer exists'], [409, 'cannot change your own role'], [500, 'could not be confirmed'],
])('explains a loading error %s without raw server details', async (status, message) => {
  listUsers.mockRejectedValue({ status, detail: 'Internal database detail' })
  render(<AdminPage session={session} />)
  expect((await screen.findByRole('alert')).textContent).toContain(message)
  expect(screen.queryByRole('table')).toBeNull()
  expect(screen.queryByText('Internal database detail')).toBeNull()
  expect(screen.queryByText('rider@example.com')).toBeNull()
})

it('retries the list after a connection error', async () => {
  listUsers.mockRejectedValueOnce({ kind: 'network' })
  render(<AdminPage session={session} />)
  expect((await screen.findByRole('alert')).textContent).toContain('Check your connection')
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByRole('table', { name: 'User accounts' })).toBeTruthy()
})

it('keeps your account and other administrators read-only', async () => {
  render(<AdminPage session={session} />)
  expect(await screen.findByText('Your account — role cannot be changed here')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Change role for @owner' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Change role for @other_admin' })).toBeNull()
  expect(screen.getByText('Administrator — read only')).toBeTruthy()
})

it.each([
  ['rider', 2, 'USER', 'MODERATOR', 'User', 'Moderator'],
  ['reviewer', 3, 'MODERATOR', 'USER', 'Moderator', 'User'],
])('confirms the role change for %s and updates the row only after 204', async (username, id, oldRole, newRole, oldLabel, newLabel) => {
  let complete
  changeUserRole.mockReturnValue(new Promise((resolve) => { complete = resolve }))
  render(<AdminPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: `Change role for @${username}` }))
  const select = screen.getByRole('combobox', { name: 'New role' })
  expect(document.activeElement).toBe(select)
  expect(select.value).toBe(oldRole)
  expect(screen.getByRole('button', { name: 'Confirm role change' }).disabled).toBe(true)
  expect(screen.queryByRole('option', { name: 'Administrator' })).toBeNull()
  fireEvent.change(select, { target: { value: newRole } })
  expect(screen.getByText(`Confirm changing @${username} from ${oldLabel} to ${newLabel}.`)).toBeTruthy()
  expect(screen.getByText(/Existing sessions keep their current permissions/)).toBeTruthy()
  expect(changeUserRole).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm role change' }))
  expect(within(screen.getByRole('row', { name: new RegExp(username) })).getByRole('cell', { name: oldLabel, exact: true })).toBeTruthy()
  expect(screen.queryByText(/now has the/)).toBeNull()
  const saving = screen.getByRole('button', { name: 'Saving…' })
  expect(saving.disabled).toBe(true)
  fireEvent.click(saving)
  expect(screen.getByRole('button', { name: 'Refresh accounts' }).disabled).toBe(true)
  expect(changeUserRole).toHaveBeenCalledExactlyOnceWith(id, newRole)
  await act(async () => { complete(null) })
  expect(await screen.findByText(`@${username} now has the ${newLabel} role for future logins. Existing sessions are unchanged.`)).toBeTruthy()
  expect(within(screen.getByRole('row', { name: new RegExp(username) })).getByRole('cell', { name: newLabel, exact: true })).toBeTruthy()
  expect(screen.getByText(`${username}@example.com`)).toBeTruthy()
  expect(screen.queryByRole('combobox')).toBeNull()
})

it('allows cancellation and resets the form when selecting a different account', async () => {
  render(<AdminPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Change role for @rider' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MODERATOR' } })
  fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(screen.queryByRole('combobox')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Change role for @reviewer' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'USER' } })
  fireEvent.click(screen.getByRole('button', { name: 'Change role for @rider' }))
  expect(screen.getByRole('combobox').value).toBe('USER')
  expect(changeUserRole).not.toHaveBeenCalled()
})

it.each([
  [400, 'Check the selected role'], [404, 'no longer exists'], [409, 'cannot change your own role'],
])('keeps the existing role on HTTP %s and explains the failure', async (status, message) => {
  changeUserRole.mockRejectedValue({ status })
  render(<AdminPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Change role for @rider' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MODERATOR' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm role change' }))
  expect((await screen.findByRole('alert')).textContent).toContain(message)
  expect(within(screen.getByRole('row', { name: /rider/ })).getByRole('cell', { name: 'User', exact: true })).toBeTruthy()
  expect(screen.queryByText(/now has the/)).toBeNull()
  expect(screen.getByRole('combobox').value).toBe('MODERATOR')
  expect(screen.getByRole('button', { name: 'Confirm role change' }).disabled).toBe(status !== 400)
  fireEvent.click(screen.getByRole('button', { name: 'Refresh accounts' }))
  await screen.findByRole('table')
  expect(screen.queryByRole('alert')).toBeNull()
})

it.each([401, 403])('hides the list if the server denies a role change (%s)', async (status) => {
  changeUserRole.mockRejectedValue({ status })
  render(<AdminPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Change role for @rider' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MODERATOR' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm role change' }))
  expect((await screen.findByRole('alert')).textContent).toContain(status === 401 ? 'Log in' : 'does not have permission')
  expect(screen.queryByRole('table')).toBeNull()
  expect(screen.queryByRole('combobox')).toBeNull()
  expect(screen.queryByText('rider@example.com')).toBeNull()
})

it.each([{ kind: 'network' }, { kind: 'timeout' }, { status: 500 }])('requires a refresh after an uncertain update (%s)', async (error) => {
  changeUserRole.mockRejectedValue(error)
  render(<AdminPage session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Change role for @rider' }))
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'MODERATOR' } })
  fireEvent.click(screen.getByRole('button', { name: 'Confirm role change' }))
  await screen.findByRole('alert')
  expect(screen.getByRole('button', { name: 'Confirm role change' }).disabled).toBe(true)
  expect(screen.queryByText(/now has the/)).toBeNull()
  listUsers.mockResolvedValue(users.map((user) => user.id === 2 ? { ...user, role: 'MODERATOR' } : user))
  fireEvent.click(screen.getByRole('button', { name: 'Refresh accounts' }))
  await waitFor(() => expect(within(screen.getByRole('row', { name: /rider/ })).getByRole('cell', { name: 'Moderator', exact: true })).toBeTruthy())
})

it('ignores a late response after losing access', async () => {
  let complete
  listUsers.mockReturnValue(new Promise((resolve) => { complete = resolve }))
  const { rerender } = render(<AdminPage session={session} />)
  rerender(<AdminPage session={null} />)
  await act(async () => { complete(users) })
  expect(screen.queryByRole('table')).toBeNull()
  expect(screen.queryByText('rider@example.com')).toBeNull()
  expect(screen.getByRole('alert').textContent).toContain('Log in')
})
