import { render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import MyBikesPage from './MyBikesPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  listMyBikes: vi.fn(),
}))

import { listMyBikes } from '../../services/myBikes.js'

it('shows the saved bikes and their statuses', async () => {
  listMyBikes.mockResolvedValue([
    {
      id: 7,
      brand: 'Orange',
      model: 'Stage 6',
      modelYear: 2020,
      category: 'ENDURO',
      photoUrl: 'https://example.com/stage.jpg',
      status: 'PRIVATE',
      analyzed: true,
    },
  ])

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy())
  expect(screen.getByText('Private')).toBeTruthy()
  expect(screen.getByText('Analysis ready')).toBeTruthy()
  expect(screen.getByRole('img', { name: 'Photo of Orange Stage 6' })).toBeTruthy()
})

it('shows an empty state when the user has no bikes', async () => {
  listMyBikes.mockResolvedValue([])

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByText('You have not saved a bike yet. Start an analysis to create one.')).toBeTruthy())
})

it('shows a retryable error when the request fails', async () => {
  listMyBikes.mockRejectedValue({ status: 500 })

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('could not load your bikes'))
})
