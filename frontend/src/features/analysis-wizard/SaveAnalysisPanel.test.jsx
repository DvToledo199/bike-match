import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import SaveAnalysisPanel from './SaveAnalysisPanel.jsx'

const save = vi.hoisted(() => vi.fn())
vi.mock('../../services/saveAnalysis.js', () => ({ createAnalysisSaver: () => ({
  checkpoint: {}, save,
}) }))
beforeEach(() => { save.mockReset() })

it('offers account links to a guest without sending the photo or generating an explanation', () => {
  render(<SaveAnalysisPanel session={null} />)
  expect(screen.getByRole('link', { name: 'Already have an account? Log in' }).getAttribute('href')).toBe('#/login')
  expect(screen.getByRole('link', { name: 'Sign up free and save your bike' }).getAttribute('href')).toBe('#/register')
  expect(screen.getByText(/Your photo, marks and measurements will stay here/)).toBeTruthy()
  expect(save).not.toHaveBeenCalled()
})

it('shows the photo being saved and keeps storage details out of the notice', () => {
  render(<SaveAnalysisPanel session={{ username: 'rider' }} wizardData={{ photo: { previewUrl: 'blob:bike-photo' } }} />)
  expect(screen.getByRole('img', { name: 'Photo of the bike you are saving' }).getAttribute('src')).toBe('blob:bike-photo')
  expect(screen.getByText(/stays private until you request publication/)).toBeTruthy()
  expect(screen.queryByText(/Cloudinary/)).toBeNull()
})

it('passes metadata to the workflow and opens the saved bike', async () => {
  save.mockResolvedValue({ bikeId: 42, explanationReady: true })
  const onSaved = vi.fn(), onLockedChange = vi.fn()
  render(<SaveAnalysisPanel session={{ username: 'rider' }} wizardData={{}} onSaved={onSaved} onLockedChange={onLockedChange} />)
  fireEvent.change(screen.getByLabelText('Brand'), { target: { value: 'Orange' } })
  fireEvent.change(screen.getByLabelText('Model'), { target: { value: 'Stage 6' } })
  fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'ENDURO' } })
  fireEvent.change(screen.getByLabelText('Cassette'), { target: { value: 'TWELVE_SPEED' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save bike' }))
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(42))
  expect(onLockedChange).toHaveBeenCalledWith(true)
  expect(screen.getByRole('link', { name: 'Open saved bike' }).getAttribute('href')).toBe('#/bikes/42')
})
