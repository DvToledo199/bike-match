import { useState } from 'react'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App.jsx'

vi.mock('./features/analysis-wizard/AnalysisWizard.jsx', () => ({ default: function Draft() {
  const [value, setValue] = useState('')
  return <input aria-label="Draft measurement" value={value} onChange={(event) => setValue(event.target.value)} />
} }))
vi.mock('./features/catalog/CatalogPage.jsx', () => ({ default: () => <h1>Public bikes</h1> }))

afterEach(() => { window.history.replaceState(null, '', '/'); sessionStorage.clear() })

async function visit(hash) {
  await act(async () => { window.location.hash = hash; window.dispatchEvent(new HashChangeEvent('hashchange')) })
}

it('opens home, preserves the analysis across catalog and login, and returns home through the brand', async () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Know your bike.Find your flow.' })).toBeTruthy()
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
  expect(screen.getByRole('heading', { name: 'Know your bike.Find your flow.' })).toBeTruthy()
})
