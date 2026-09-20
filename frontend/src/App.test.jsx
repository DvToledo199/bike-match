import { useState } from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { listPublicBikes } from './services/myBikes.js'
import { loginUser } from './services/authLogin.js'
import { listPendingBikes } from './services/moderation.js'
import { listUsers } from './services/admin.js'

vi.mock('./services/admin.js', () => ({
  listUsers: vi.fn().mockResolvedValue([]), changeUserRole: vi.fn(),
}))

vi.mock('./services/moderation.js', () => ({
  listPendingBikes: vi.fn().mockResolvedValue([]),
  approveBike: vi.fn(), rejectBike: vi.fn(), removeBike: vi.fn(),
}))

vi.mock('./features/analysis-wizard/AnalysisWizard.jsx', () => ({ default: function Draft() {
  const [value, setValue] = useState('')
  return <input aria-label="Draft measurement" value={value} onChange={(event) => setValue(event.target.value)} />
} }))
vi.mock('./features/catalog/CatalogPage.jsx', () => ({ default: () => <h1>Public bikes</h1> }))
vi.mock('./services/myBikes.js', () => ({
  listPublicBikes: vi.fn().mockResolvedValue({ items: [], page: 0, totalPages: 0, hasNext: false }),
  listMyBikes: vi.fn().mockResolvedValue([]),
  listMyNotices: vi.fn().mockResolvedValue([]),
  publishBike: vi.fn(),
  deleteBike: vi.fn(),
  dismissNotice: vi.fn(),
}))
vi.mock('./services/authLogin.js', () => ({ loginUser: vi.fn() }))
vi.mock('./features/my-bikes/BikeDetailPage.jsx', () => ({ default: ({ onBack, backLabelKey }) => (
  <section><h1>Saved bike detail</h1><button onClick={onBack}>{backLabelKey}</button></section>
) }))

afterEach(() => { window.history.replaceState(null, '', '/'); sessionStorage.clear() })

async function visit(hash) {
  await act(async () => { window.location.hash = hash; window.dispatchEvent(new HashChangeEvent('hashchange')) })
}

it('opens the moderation route for a moderator', async () => {
  sessionStorage.setItem('bikematch.session', JSON.stringify({ accessToken: 'token', role: 'MODERATOR' }))
  window.history.replaceState(null, '', '/#/moderation')
  render(<App />)
  expect(await screen.findByText('No bikes are waiting for review.')).toBeTruthy()
  expect(listPendingBikes).toHaveBeenCalled()
})

it('opens administration for an administrator', async () => {
  sessionStorage.setItem('bikematch.session', JSON.stringify({ accessToken: 'token', username: 'owner', role: 'ADMIN' }))
  window.history.replaceState(null, '', '/#/admin')
  render(<App />)
  expect(await screen.findByText('There are no accounts to display.')).toBeTruthy()
  expect(listUsers).toHaveBeenCalled()
})

it('refuses administration when a moderator forces the route', () => {
  sessionStorage.setItem('bikematch.session', JSON.stringify({ accessToken: 'token', role: 'MODERATOR' }))
  window.history.replaceState(null, '', '/#/admin')
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Account administration' })).toBeTruthy()
  expect(screen.getByRole('alert').textContent).toContain('does not have permission')
  expect(screen.queryByRole('table')).toBeNull()
})

it('explains a forced moderation route without a session', () => {
  window.history.replaceState(null, '', '/#/moderation')
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Bike moderation' })).toBeTruthy()
  expect(screen.getByRole('alert').textContent).toContain('Log in')
})

function signIn() {
  loginUser.mockResolvedValue({
    accessToken: `header.${btoa(JSON.stringify({ username: 'david', role: 'USER' }))}.signature`,
    tokenType: 'Bearer',
  })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'david@example.com' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'BikeMatch8!' } })
  fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
}

it('opens home, preserves the analysis across catalog and login, and returns home through the brand', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Bikes worth a closer look.' })).toBeTruthy()
  await visit('/analyze')
  fireEvent.change(screen.getByRole('textbox', { name: 'Draft measurement' }), { target: { value: '230' } })
  await visit('/catalog')
  expect(screen.getByRole('heading', { name: 'Public bikes' })).toBeTruthy()
  expect(screen.queryByRole('textbox', { name: 'Draft measurement' })).toBeNull()
  await visit('/login')
  expect(screen.getByRole('heading', { name: 'Log in' })).toBeTruthy()
  fireEvent.click(screen.getByRole('link', { name: 'Skip to main content' }))
  expect(window.location.hash).toBe('#/login')
  await visit('/analyze')
  expect(screen.getByRole('textbox', { name: 'Draft measurement' }).value).toBe('230')
  expect(screen.getByRole('link', { name: 'BikeMatch home' }).getAttribute('href')).toBe('#/')
  await visit('/')
  expect(screen.getByRole('heading', { name: 'Bikes worth a closer look.' })).toBeTruthy()
})

it('returns to the community after opening a photo from the home feed', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  listPublicBikes.mockResolvedValue({ items: [{ id: 7, brand: 'Orange', model: 'Stage 6' }], page: 0, totalPages: 1, hasNext: false })
  render(<App />)
  fireEvent.click(await screen.findByRole('link', { name: 'View analysis of Orange Stage 6' }))
  expect(await screen.findByRole('heading', { name: 'Saved bike detail' })).toBeTruthy()
  expect(window.location.hash).toBe('#/bikes/7')
  fireEvent.click(screen.getByRole('button', { name: 'bikeDetail.backHome' }))
  expect(await screen.findByRole('heading', { name: 'Bikes worth a closer look.' })).toBeTruthy()
  expect(window.location.hash).toBe('#/')
})

it('returns to the screen you came from after logging in', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  render(<App />)
  await visit('/catalog')
  await visit('/login')

  signIn()

  await waitFor(() => expect(window.location.hash).toBe('#/catalog'))
})

it('opens my bikes after logging in from the my bikes screen', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  render(<App />)
  await visit('/my-bikes')
  expect(screen.getByRole('heading', { name: 'Log in' })).toBeTruthy()

  signIn()

  expect(await screen.findByRole('heading', { name: 'My bikes' })).toBeTruthy()
  expect(window.location.hash).toBe('#/my-bikes')
})
