import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import ParameterStep from './ParameterStep.jsx'
import KinematicsCharts from './KinematicsCharts.jsx'
import { referencePreview } from '../../test/referencePreview.js'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  LineChart: ({ children }) => <div>{children}</div>,
  CartesianGrid: () => null, Line: () => null, Tooltip: () => null, XAxis: () => null, YAxis: () => null,
  ReferenceLine: () => <span>100% reference</span>,
}))

it('offers exactly three explicit wheel choices and keeps the other parameters', () => {
  const updateWizardData = vi.fn()
  render(<ParameterStep parameters={{ eyeToEyeMm: '210', wheelConfiguration: '' }} points={{}} updateWizardData={updateWizardData} />)
  const select = screen.getByLabelText('Wheel setup')
  expect(select.value).toBe('')
  expect(screen.getAllByRole('option')).toHaveLength(4)
  fireEvent.change(select, { target: { value: 'MULLET' } })
  expect(updateWizardData).toHaveBeenCalledWith({ parameters: { eyeToEyeMm: '210', wheelConfiguration: 'MULLET' } })
  fireEvent.blur(select)
  expect(screen.getByRole('alert').textContent).toContain('Select the wheel setup')
})

it('shows five labelled graphs, endpoint summaries and honest reference conditions', () => {
  const { container } = render(<KinematicsCharts data={referencePreview()} />)
  expect(screen.getByRole('heading', { name: 'Anti-squat — pedalling response' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'Anti-rise — braking response' })).toBeTruthy()
  expect(screen.getByText(/Starts at 120.5% and ends at -10.2%/)).toBeTruthy()
  expect(screen.getByText(/Starts at 80.4% and ends at 60.1%/)).toBeTruthy()
  expect(screen.getByText(/centre-of-gravity height 1.10 m/)).toBeTruthy()
  expect(screen.getByText(/not a personalized ride prediction/)).toBeTruthy()
  expect(screen.getByText(/External validation of kickback, anti-squat and anti-rise is still in progress/)).toBeTruthy()
  expect(screen.queryByText(/does not affect the v1/)).toBeNull()
  expect(container.querySelectorAll('table')).toHaveLength(0)
})
