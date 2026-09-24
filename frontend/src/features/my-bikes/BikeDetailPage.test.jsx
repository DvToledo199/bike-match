import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import BikeDetailPage from './BikeDetailPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  getBikeDetail: vi.fn(),
  toKinematicsData: vi.fn(),
}))

vi.mock('../../services/interpretation.js', () => ({
  getBikeInterpretation: vi.fn(),
  generateBikeInterpretation: vi.fn(),
}))

vi.mock('../analysis-wizard/KinematicsCharts.jsx', () => ({
  default: () => <div role="region" aria-label="Saved kinematics charts" />,
}))

import { getBikeDetail, toKinematicsData } from '../../services/myBikes.js'
import { generateBikeInterpretation, getBikeInterpretation } from '../../services/interpretation.js'

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

const explanation = {
  resultVersion: 1,
  interpretationContextVersion: 1,
  rulesVersion: 'kinematics-rules-1',
  language: 'en',
  source: 'RULES',
  providerVersion: 'rules-1',
  summary: 'This analysis shows a progressive response.',
  evidence: [
    { key: 'usefulProgressionPercent', value: 18, unit: '%' },
    { key: 'maxRearwardMm', value: 12, unit: 'mm' },
  ],
}

beforeEach(() => {
  vi.clearAllMocks()
  sessionStorage.clear()
  toKinematicsData.mockReturnValue({})
})

it('shows the saved bike photo, specifications and charts', async () => {
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockResolvedValue(explanation)

  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Orange Stage 6' })).toBeTruthy())
  expect(screen.getByRole('img', { name: 'Photo of Orange Stage 6' })).toBeTruthy()
  expect(screen.getByText('Bike specifications')).toBeTruthy()
  await waitFor(() => expect(screen.getByText('This analysis shows a progressive response.')).toBeTruthy())
  expect(screen.getByText('Useful progression')).toBeTruthy()
  // Where the text comes from heads the card, and the note about its limits is said once.
  expect(screen.getByText('Rules-based summary')).toBeTruthy()
  expect(screen.getByText('Note:')).toBeTruthy()
  expect(screen.getByRole('region', { name: 'Saved kinematics charts' })).toBeTruthy()
  expect(getBikeDetail).toHaveBeenCalledWith(7)
  expect(getBikeInterpretation).toHaveBeenCalledWith(7, 'en')
})

it('names every figure the explanation cites', async () => {
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockResolvedValue({
    ...explanation,
    evidence: [
      { key: 'usefulProgressionPercent', value: 2.5, unit: '%' },
      { key: 'antiSquatAtSagPercent', value: 99.1, unit: '%' },
      { key: 'antiRiseAtSagPercent', value: 80, unit: '%' },
      { key: 'totalProgressionPercent', value: 3.3, unit: '%' },
    ],
  })

  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await waitFor(() => expect(screen.getByText('Anti-squat at sag')).toBeTruthy())
  expect(screen.getByText('Anti-rise at sag')).toBeTruthy()
  expect(screen.getByText('Total progression')).toBeTruthy()
  expect(screen.queryByText('Supporting value')).toBeNull()
})

it('lets the owner generate the explanation when it is missing', async () => {
  sessionStorage.setItem('bikematch.session', JSON.stringify({
    accessToken: 'token',
    tokenType: 'Bearer',
    username: 'david',
  }))
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockRejectedValue({ status: 404 })
  generateBikeInterpretation.mockResolvedValue(explanation)

  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await waitFor(() => expect(screen.getByRole('button', { name: 'Generate explanation' })).toBeTruthy())
  screen.getByRole('button', { name: 'Generate explanation' }).click()

  await waitFor(() => expect(screen.getByText('This analysis shows a progressive response.')).toBeTruthy())
  expect(generateBikeInterpretation).toHaveBeenCalledWith(7, 'en')
})

it('explains when the bike cannot be accessed and allows going back', async () => {
  const onBack = vi.fn()
  getBikeDetail.mockRejectedValue({ status: 404 })
  getBikeInterpretation.mockResolvedValue(explanation)

  render(<BikeDetailPage bikeId={7} onBack={onBack} />)

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('private'))
  screen.getByRole('button', { name: 'Back to my bikes' }).click()

  expect(onBack).toHaveBeenCalled()
})
