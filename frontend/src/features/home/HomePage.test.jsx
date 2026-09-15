import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import HomePage from './HomePage.jsx'
import { listPublicBikes } from '../../services/myBikes.js'

vi.mock('../../services/myBikes.js', () => ({ listPublicBikes: vi.fn() }))

const firstPage = {
  items: [{ id: 7, brand: 'Orange', model: 'Stage 6', modelYear: 2020, category: 'ENDURO', photoUrl: 'https://example.com/bike.jpg' }],
  page: 0, totalPages: 2, hasNext: true,
}

beforeEach(() => { vi.resetAllMocks() })

it('uses public photos, opens a bike and contains no decorative illustration', async () => {
  listPublicBikes.mockResolvedValue(firstPage)
  const onOpenBikeDetail = vi.fn()
  const { container } = render(<HomePage onOpenBikeDetail={onOpenBikeDetail} />)
  const photo = await screen.findByRole('img', { name: 'Photo of Orange Stage 6' })
  expect(photo.getAttribute('src')).toBe(firstPage.items[0].photoUrl)
  expect(container.querySelector('svg')).toBeNull()
  expect(listPublicBikes).toHaveBeenCalledWith({ category: '', page: 0 })
  const link = screen.getByRole('link', { name: 'View analysis of Orange Stage 6' })
  expect(link.getAttribute('href')).toBe('#/bikes/7')
  fireEvent.click(link)
  expect(onOpenBikeDetail).toHaveBeenCalledWith(7)
})

it('shows an honest empty state with no invented bikes', async () => {
  listPublicBikes.mockResolvedValue({ ...firstPage, items: [], totalPages: 0, hasNext: false })
  render(<HomePage />)
  expect(await screen.findByRole('heading', { name: 'The community starts here.' })).toBeTruthy()
  expect(screen.queryByRole('img')).toBeNull()
  expect(screen.queryByRole('navigation')).toBeNull()
  expect(screen.getAllByRole('link', { name: /Analyze your bike/ }).every((link) => link.getAttribute('href') === '#/analyze')).toBe(true)
})

it('paginates and resets to the first page when changing category', async () => {
  listPublicBikes.mockResolvedValueOnce(firstPage)
    .mockResolvedValueOnce({ ...firstPage, page: 1, hasNext: false })
    .mockResolvedValue({ ...firstPage, items: [], page: 0, hasNext: false, totalPages: 0 })
  render(<HomePage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Next' }))
  await waitFor(() => expect(listPublicBikes).toHaveBeenLastCalledWith({ category: '', page: 1 }))
  expect((await screen.findByRole('button', { name: 'Next' })).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Downhill' }))
  await waitFor(() => expect(listPublicBikes).toHaveBeenLastCalledWith({ category: 'DOWNHILL', page: 0 }))
  expect(await screen.findByRole('heading', { name: 'No bikes in this category yet.' })).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Show all bikes' }))
  await waitFor(() => expect(listPublicBikes).toHaveBeenLastCalledWith({ category: '', page: 0 }))
})

it('does not confuse a server error with an empty community and offers retry', async () => {
  listPublicBikes.mockRejectedValueOnce(new Error('offline')).mockResolvedValue(firstPage)
  render(<HomePage />)
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.queryByText('The community starts here.')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByRole('img')).toBeTruthy()
})

it('shows a missing photo state when the image fails without hiding the bike', async () => {
  listPublicBikes.mockResolvedValue(firstPage)
  render(<HomePage />)
  fireEvent.error(await screen.findByRole('img'))
  expect(screen.queryByRole('img')).toBeNull()
  expect(screen.getByRole('link', { name: 'View analysis of Orange Stage 6' })).toBeTruthy()
})

it('ignores an obsolete category response arriving after the newer one', async () => {
  let resolveOld
  listPublicBikes.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve }))
    .mockResolvedValueOnce({ ...firstPage, items: [], totalPages: 0, hasNext: false })
  render(<HomePage />)
  fireEvent.click(screen.getByRole('button', { name: 'Downhill' }))
  await screen.findByRole('heading', { name: 'No bikes in this category yet.' })
  await act(async () => { resolveOld(firstPage) })
  expect(screen.queryByRole('img')).toBeNull()
})
