import { useState } from 'react'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App.jsx'
import { listPublicBikes } from './services/myBikes.js'

vi.mock('./features/analysis-wizard/AnalysisWizard.jsx', () => ({ default: function Draft() {
  const [value, setValue] = useState('')
  return <input aria-label="Draft measurement" value={value} onChange={(event) => setValue(event.target.value)} />
} }))
vi.mock('./features/catalog/CatalogPage.jsx', () => ({ default: () => <h1>Public bikes</h1> }))
vi.mock('./services/myBikes.js', () => ({ listPublicBikes: vi.fn().mockResolvedValue({ items: [], page: 0, totalPages: 0, hasNext: false }) }))
vi.mock('./features/my-bikes/BikeDetailPage.jsx', () => ({ default: ({ onBack, backLabelKey }) => (
  <section><h1>Saved bike detail</h1><button onClick={onBack}>{backLabelKey}</button></section>
) }))

afterEach(() => { window.history.replaceState(null, '', '/'); sessionStorage.clear() })

async function visit(hash) {
  await act(async () => { window.location.hash = hash; window.dispatchEvent(new HashChangeEvent('hashchange')) })
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
