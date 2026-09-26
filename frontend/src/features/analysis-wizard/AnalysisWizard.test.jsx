import { fireEvent, render, screen, within } from '@testing-library/react'
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

it('clears incompatible marks and the stale preview when the layout changes', () => {
  state.wizard.activeStepIndex = 0
  state.wizard.wizardData = {
    ...state.wizard.wizardData,
    suspensionLayout: 'SINGLE_PIVOT',
    points: { MAIN_PIVOT: { type: 'MAIN_PIVOT', x: 10, y: 10 } },
  }
  state.preview.data = {}
  render(<AnalysisWizard />)

  fireEvent.click(screen.getByRole('radio', { name: 'Four-bar (Horst link)' }))

  expect(state.preview.cancelPreview).toHaveBeenCalledOnce()
  expect(state.wizard.updateWizardData).toHaveBeenCalledWith({
    suspensionLayout: 'HORST_LINK',
    points: {},
  })
})

it('clears incompatible marks and the stale preview when the yoke layout is selected', () => {
  state.wizard.activeStepIndex = 0
  state.wizard.wizardData = {
    ...state.wizard.wizardData,
    suspensionLayout: 'HORST_LINK',
    points: { SHOCK_ROCKER: { type: 'SHOCK_ROCKER', x: 10, y: 10 } },
  }
  state.preview.data = {}
  render(<AnalysisWizard />)

  fireEvent.click(screen.getByRole('radio', { name: 'Four-bar (Horst link with rigid yoke)' }))

  expect(state.preview.cancelPreview).toHaveBeenCalledOnce()
  expect(state.wizard.updateWizardData).toHaveBeenCalledWith({
    suspensionLayout: 'HORST_LINK_YOKE',
    points: {},
  })
})

it('shows explicit registration actions above and below completed curves for a guest', () => {
  state.preview.data = {}
  render(<AnalysisWizard />)
  const links = screen.getAllByRole('link', { name: 'Sign up free and save your bike' })
  expect(links).toHaveLength(2)
  expect(links.every((link) => link.getAttribute('href') === '#/register')).toBe(true)
  const curves = screen.getByRole('heading', { name: 'Calculated curves' })
  expect(links[0].compareDocumentPosition(curves) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  expect(curves.compareDocumentPosition(links[1]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  expect(screen.getAllByRole('link', { name: 'Already have an account? Log in' })).toHaveLength(2)
})

it('offers the signed-in user a save form and a footer shortcut that focuses it', () => {
  state.preview.data = {}
  render(<AnalysisWizard session={{ username: 'rider' }} />)
  expect(screen.queryByRole('link', { name: 'Sign up free and save your bike' })).toBeNull()
  const form = screen.getByRole('region', { name: 'Save your bike to see its AI summary' })
  expect(within(form).getByRole('button', { name: 'Save bike' }).type).toBe('submit')
  const panel = document.getElementById('save-analysis-panel')
  panel.scrollIntoView = vi.fn()
  fireEvent.click(within(screen.getByRole('region', { name: 'Save your results' })).getByRole('button', { name: 'Save bike' }))
  expect(document.activeElement).toBe(panel)
  expect(panel.scrollIntoView).toHaveBeenCalledWith({ block: 'start' })
  expect(state.preview.requestPreview).not.toHaveBeenCalled()
})

it.each([{ isLoading: true }, { error: { kind: 'network' } }])('does not offer to save an unfinished calculation: %j', (preview) => {
  Object.assign(state.preview, preview)
  render(<AnalysisWizard />)
  expect(screen.queryByRole('link', { name: 'Sign up free and save your bike' })).toBeNull()
  expect(screen.queryByRole('region', { name: 'Save your results' })).toBeNull()
})

it('updates saving actions when a guest logs in without recalculating', () => {
  state.preview.data = {}
  const { rerender } = render(<AnalysisWizard />)
  expect(screen.getAllByRole('link', { name: 'Sign up free and save your bike' })).toHaveLength(2)
  rerender(<AnalysisWizard session={{ username: 'rider' }} />)
  expect(screen.getByRole('heading', { name: 'Calculated curves' })).toBeTruthy()
  expect(screen.queryByRole('link', { name: 'Sign up free and save your bike' })).toBeNull()
  expect(screen.getAllByRole('button', { name: 'Save bike' })).toHaveLength(2)
  expect(state.preview.requestPreview).not.toHaveBeenCalled()
})
