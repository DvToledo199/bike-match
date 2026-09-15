import { render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import MyBikesPage from './MyBikesPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  listMyBikes: vi.fn(),
  publishBike: vi.fn(),
  deleteBike: vi.fn(),
}))

import { deleteBike, listMyBikes, publishBike } from '../../services/myBikes.js'

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

it('opens the selected bike detail', async () => {
  const onOpenBikeDetail = vi.fn()
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PRIVATE',
    analyzed: true,
  }])

  render(<MyBikesPage onOpenBikeDetail={onOpenBikeDetail} />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Open bike detail' })).toBeTruthy())
  screen.getByRole('button', { name: 'Open bike detail' }).click()

  expect(onOpenBikeDetail).toHaveBeenCalledWith(7)
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

it('confirms publication and updates the bike to pending review', async () => {
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PRIVATE',
    analyzed: true,
  }])
  publishBike.mockResolvedValue({ id: 7, status: 'PENDING' })

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Request publication' })).toBeTruthy())
  screen.getByRole('button', { name: 'Request publication' }).click()
  await waitFor(() => expect(screen.getByText(/Send this bike to moderation/)).toBeTruthy())
  expect(screen.getByText(/took this photo or have permission from its author/)).toBeTruthy()
  screen.getByRole('button', { name: 'Confirm publication' }).click()

  await waitFor(() => expect(screen.getByText('Pending review')).toBeTruthy())
  expect(publishBike).toHaveBeenCalledWith(7)
  expect(screen.queryByRole('button', { name: 'Request publication' })).toBeNull()
})

it('confirms deletion and removes the bike from the list', async () => {
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PUBLIC',
    analyzed: true,
  }])
  deleteBike.mockResolvedValue(null)

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete bike' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete bike' }).click()
  await waitFor(() => expect(screen.getByText(/Delete this bike permanently\? .* This cannot be undone\./)).toBeTruthy())
  screen.getByRole('button', { name: 'Delete permanently' }).click()

  await waitFor(() => expect(screen.getByText('You have not saved a bike yet. Start an analysis to create one.')).toBeTruthy())
  expect(deleteBike).toHaveBeenCalledWith(7)
})

it('keeps the bike when the deletion is cancelled', async () => {
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PRIVATE',
    analyzed: false,
  }])

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete bike' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete bike' }).click()
  await waitFor(() => expect(screen.getByRole('button', { name: 'Keep bike' })).toBeTruthy())
  screen.getByRole('button', { name: 'Keep bike' }).click()

  await waitFor(() => expect(screen.queryByText(/Delete this bike permanently/)).toBeNull())
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Delete bike' })).toBeTruthy()
})

it('keeps the bike and explains when the deletion fails', async () => {
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PRIVATE',
    analyzed: true,
  }])
  deleteBike.mockRejectedValue({ status: 500 })

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete bike' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete bike' }).click()
  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete permanently' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete permanently' }).click()

  await waitFor(() => expect(screen.getByRole('alert').textContent).toBe('We could not delete this bike right now. Try again.'))
  expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy()
})

it('removes a bike that was already deleted elsewhere', async () => {
  listMyBikes.mockResolvedValue([{
    id: 7,
    brand: 'Orange',
    model: 'Stage 6',
    modelYear: 2020,
    category: 'ENDURO',
    photoUrl: null,
    status: 'PRIVATE',
    analyzed: true,
  }])
  deleteBike.mockRejectedValue({ status: 404 })

  render(<MyBikesPage />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete bike' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete bike' }).click()
  await waitFor(() => expect(screen.getByRole('button', { name: 'Delete permanently' })).toBeTruthy())
  screen.getByRole('button', { name: 'Delete permanently' }).click()

  await waitFor(() => expect(screen.getByText('You have not saved a bike yet. Start an analysis to create one.')).toBeTruthy())
  expect(screen.queryByRole('alert')).toBeNull()
})
