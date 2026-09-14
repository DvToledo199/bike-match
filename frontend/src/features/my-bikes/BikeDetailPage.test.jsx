import { render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import BikeDetailPage from './BikeDetailPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  getBikeDetail: vi.fn(),
  toKinematicsData: vi.fn(),
}))

vi.mock('../analysis-wizard/KinematicsCharts.jsx', () => ({
  default: () => <div role="region" aria-label="Saved kinematics charts" />,
}))

import { getBikeDetail, toKinematicsData } from '../../services/myBikes.js'

const bike = {
  id: 7,
  brand: 'Orange',
  model: 'Stage 6',
  modelYear: 2020,
  category: 'ENDURO',
  suspensionLayout: 'SINGLE_PIVOT',
  declaredTravelMm: 150,
  wheelConfiguration: 'FULL_29',
  sagPercent: 30,
  photoUrl: 'https://example.com/stage.jpg',
  status: 'PRIVATE',
  ownerUsername: 'david',
  result: { curves: {}, descriptors: {} },
}

it('shows the saved bike photo, specifications and charts', async () => {
  getBikeDetail.mockResolvedValue(bike)
  toKinematicsData.mockReturnValue({})

  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy())
  expect(screen.getByRole('img', { name: 'Photo of Orange Stage 6' })).toBeTruthy()
  expect(screen.getByText('Bike specifications')).toBeTruthy()
  expect(screen.getByRole('region', { name: 'Saved kinematics charts' })).toBeTruthy()
  expect(getBikeDetail).toHaveBeenCalledWith(7)
})

it('explains when the bike cannot be accessed and allows going back', async () => {
  const onBack = vi.fn()
  getBikeDetail.mockRejectedValue({ status: 404 })

  render(<BikeDetailPage bikeId={7} onBack={onBack} />)

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('private'))
  screen.getByRole('button', { name: 'Back to my bikes' }).click()

  expect(onBack).toHaveBeenCalled()
})
