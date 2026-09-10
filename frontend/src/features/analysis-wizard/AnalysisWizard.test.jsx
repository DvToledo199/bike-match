import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import AnalysisWizard from './AnalysisWizard.jsx'

const state = vi.hoisted(() => ({ wizard: {}, preview: {} }))
vi.mock('./useWizardState.js', () => ({ default: () => state.wizard }))
vi.mock('./usePreview.js', () => ({ default: () => state.preview }))
vi.mock('./KinematicsCharts.jsx', () => ({ default: () => <h1 id="wizard-title">Calculated curves</h1> }))

beforeEach(() => {
  state.wizard = {
    activeStepIndex: 3,
    wizardData: {
      photo: null,
      points: { SHOCK_FRAME: { x: 10, y: 10 }, SHOCK_SWINGARM: { x: 210, y: 10 } },
      parameters: { eyeToEyeMm: '210', shockStrokeMm: '55', chainringTeeth: '32',
        sprocketTeeth: '50', declaredTravelMm: '150', sagPercent: '30', wheelConfiguration: 'FULL_29' },
    },
    updateWizardData: vi.fn(), goToNextStep: vi.fn(), goToPreviousStep: vi.fn(),
  }
  state.preview = { data: null, error: null, isLoading: false, requestPreview: vi.fn(), cancelPreview: vi.fn() }
})

it.each([
  [{ isLoading: true }, 'Calculating your bike'],
  [{ data: {} }, 'Calculated curves'],
  [{ error: { kind: 'network' } }, 'We could not calculate this bike'],
])('has no redundant forward action in results state %j', (preview, heading) => {
  Object.assign(state.preview, preview)
  render(<AnalysisWizard />)
  expect(screen.getByRole('heading', { name: heading })).toBeTruthy()
  expect(screen.queryByRole('button', { name: /View results|Next|Calculate kinematics/ })).toBeNull()
  expect(screen.getByRole('button', { name: 'Back' }).disabled).toBe(false)
})

it('keeps the error retry action and reuses the entered bike data', () => {
  state.preview.error = { kind: 'network' }
  render(<AnalysisWizard />)
  fireEvent.click(screen.getByRole('button', { name: 'Try calculation again' }))
  expect(state.preview.requestPreview).toHaveBeenCalledWith(state.wizard.wizardData)
})

it('can return to parameters and cancels any outstanding request', () => {
  state.preview.isLoading = true
  render(<AnalysisWizard />)
  fireEvent.click(screen.getByRole('button', { name: 'Back' }))
  expect(state.preview.cancelPreview).toHaveBeenCalledOnce()
  expect(state.wizard.goToPreviousStep).toHaveBeenCalledOnce()
})

it('still calculates from valid parameters without a second confirmation', () => {
  state.wizard.activeStepIndex = 2
  render(<AnalysisWizard />)
  const calculate = screen.getByRole('button', { name: 'Calculate kinematics' })
  expect(calculate.disabled).toBe(false)
  fireEvent.click(calculate)
  expect(state.wizard.goToNextStep).toHaveBeenCalledOnce()
  expect(state.preview.requestPreview).toHaveBeenCalledWith(state.wizard.wizardData)
})
