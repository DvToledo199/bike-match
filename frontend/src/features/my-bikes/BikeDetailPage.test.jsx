import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import BikeDetailPage from './BikeDetailPage.jsx'

vi.mock('../../services/myBikes.js', () => ({
  getBikeDetail: vi.fn(),
  publishBike: vi.fn(),
  toKinematicsData: vi.fn(),
}))

vi.mock('../../services/interpretation.js', () => ({
  getBikeInterpretation: vi.fn(),
  generateBikeInterpretation: vi.fn(),
}))

vi.mock('../analysis-wizard/KinematicsCharts.jsx', () => ({
  default: () => <div role="region" aria-label="Saved kinematics charts" />,
}))

import { getBikeDetail, publishBike, toKinematicsData } from '../../services/myBikes.js'
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
  analyzed: true,
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
  // Stored codes are shown with their names, never as ENDURO or SINGLE_PIVOT.
  expect(screen.getByText('Single pivot')).toBeTruthy()
  expect(screen.getByText('Full 29″ — both wheels')).toBeTruthy()
  expect(screen.queryByText('SINGLE_PIVOT')).toBeNull()
  expect(screen.queryByText('ENDURO')).toBeNull()
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

it('says when the AI did not answer, shows the rules text as a stand-in and offers to try again', async () => {
  sessionStorage.setItem('bikematch.session', JSON.stringify({
    accessToken: 'token',
    tokenType: 'Bearer',
    username: 'david',
  }))
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockResolvedValue({ ...explanation, fallback: true })
  generateBikeInterpretation.mockResolvedValue({ ...explanation, source: 'AI', fallback: false })

  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await waitFor(() => expect(screen.getByText(/The AI could not answer right now/)).toBeTruthy())
  expect(screen.getByRole('heading', { name: 'For now, an approximate description' })).toBeTruthy()
  expect(screen.getByText('This analysis shows a progressive response.')).toBeTruthy()
  screen.getByRole('button', { name: 'Try again with AI' }).click()

  await waitFor(() => expect(screen.getByRole('heading', { name: 'What this analysis suggests' })).toBeTruthy())
  expect(screen.getByText('AI-generated summary')).toBeTruthy()
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

function logInAs(username) {
  sessionStorage.setItem('bikematch.session', JSON.stringify({ accessToken: 'token', tokenType: 'Bearer', username }))
}

it('lets the owner request publication from the detail page after confirming', async () => {
  logInAs('david')
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockResolvedValue(explanation)
  publishBike.mockResolvedValue({ id: 7, status: 'PENDING' })
  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  fireEvent.click(await screen.findByRole('button', { name: 'Request publication' }))
  expect(screen.getByRole('group', { name: /Send this bike to moderation/ })).toBeTruthy()
  expect(publishBike).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm publication' }))

  await waitFor(() => expect(screen.getByText('Pending review')).toBeTruthy())
  expect(publishBike).toHaveBeenCalledWith(7)
  expect(screen.queryByText('Private')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Request publication' })).toBeNull()
})

it('keeps the bike private when the owner cancels or the session has expired', async () => {
  logInAs('david')
  getBikeDetail.mockResolvedValue(bike)
  getBikeInterpretation.mockResolvedValue(explanation)
  publishBike.mockRejectedValue({ status: 401 })
  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  fireEvent.click(await screen.findByRole('button', { name: 'Request publication' }))
  fireEvent.click(screen.getByRole('button', { name: 'Keep private' }))
  fireEvent.click(screen.getByRole('button', { name: 'Request publication' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm publication' }))

  expect((await screen.findByRole('alert')).textContent).toBe('Your session has expired. Log in again before publishing.')
  expect(screen.getByText('Private')).toBeTruthy()
})

it.each([
  ['a visitor', 'someone_else', bike],
  ['the owner of a public bike', 'david', { ...bike, status: 'PUBLIC' }],
  ['the owner of an unanalysed bike', 'david', { ...bike, analyzed: false }],
])('does not offer publication to %s', async (_, username, shownBike) => {
  logInAs(username)
  getBikeDetail.mockResolvedValue(shownBike)
  getBikeInterpretation.mockResolvedValue(explanation)
  render(<BikeDetailPage bikeId={7} onBack={vi.fn()} />)

  await screen.findByRole('heading', { name: 'Orange Stage 6' })
  expect(screen.queryByRole('button', { name: 'Request publication' })).toBeNull()
})
