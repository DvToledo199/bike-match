import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import CatalogPage from './CatalogPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  listPublicBikes: vi.fn(),
}))

import { listPublicBikes } from '../../services/myBikes.js'

beforeEach(() => {
  vi.clearAllMocks()
})

const catalog = {
  items: [{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: 'https://example.com/stage.jpg',
  }],
  page: 0,
  totalPages: 2,
  hasNext: true,
}

/** The free host needs about a minute to wake up; the page says so instead of looking stuck. */
it('says the server is waking up when the catalog takes more than a few seconds', () => {
  vi.useFakeTimers()
  listPublicBikes.mockReturnValue(new Promise(() => {}))

  render(<CatalogPage onOpenBikeDetail={vi.fn()} />)
  expect(screen.getByText('Loading the public catalog…')).toBeTruthy()

  act(() => { vi.advanceTimersByTime(5000) })
  expect(screen.getByText(/Waking up the server/)).toBeTruthy()
})

it('shows public bikes and opens their detail', async () => {
  const onOpenBikeDetail = vi.fn()
  listPublicBikes.mockResolvedValue(catalog)

  render(<CatalogPage onOpenBikeDetail={onOpenBikeDetail} />)

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy())
  expect(screen.getByText('Community bikes')).toBeTruthy()
  expect(screen.getAllByText('Enduro')).toHaveLength(2)
  expect(screen.getByRole('img', { name: 'Photo of Orange Stage 6' })).toBeTruthy()

  screen.getByRole('link', { name: 'View analysis of Orange Stage 6' }).click()

  expect(onOpenBikeDetail).toHaveBeenCalledWith(7)
})

it('reloads the first page when the category changes', async () => {
  listPublicBikes.mockResolvedValue(catalog)

  render(<CatalogPage onOpenBikeDetail={vi.fn()} />)
  await waitFor(() => expect(listPublicBikes).toHaveBeenCalledWith({ category: '', page: 0 }))

  fireEvent.click(screen.getByRole('button', { name: 'Downhill' }))

  await waitFor(() => expect(listPublicBikes).toHaveBeenCalledWith({ category: 'DOWNHILL', page: 0 }))
})

it('can retry a failed request', async () => {
  listPublicBikes.mockRejectedValueOnce({ status: 503 }).mockResolvedValueOnce(catalog)

  render(<CatalogPage onOpenBikeDetail={vi.fn()} />)
  await waitFor(() => expect(screen.getByRole('alert')).toBeTruthy())

  screen.getByRole('button', { name: 'Try again' }).click()

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy())
  expect(listPublicBikes).toHaveBeenCalledTimes(2)
})
