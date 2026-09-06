import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import PointMarker from './PointMarker.jsx'

function MarkerTest() {
  const [points, setPoints] = useState({})
  return <><PointMarker photo={{ width: 1800, height: 1200, previewUrl: 'blob:photo' }} points={points}
    updateWizardData={(update) => setPoints(update.points)} /><output aria-label="Stored marks">{JSON.stringify(points)}</output></>
}

it('creates all six points with the keyboard, and undo removes both cross and data immediately', () => {
  const { container } = render(<MarkerTest />)
  const marker = screen.getByRole('application', { name: 'Bike photo used to mark suspension points' })
  for (let i = 0; i < 6; i++) {
    fireEvent.keyDown(marker, { key: 'ArrowRight', shiftKey: true })
    fireEvent.keyDown(marker, { key: 'Enter' })
  }
  expect(screen.getByText('6 of 6 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(6)
  fireEvent.click(screen.getByRole('button', { name: 'Undo point' }))
  expect(screen.getByText('5 of 6 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(5)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent).FRONT_AXLE).toBeUndefined()
  for (let i = 0; i < 5; i++) fireEvent.click(screen.getByRole('button', { name: 'Undo point' }))
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent)).toEqual({})
  expect(container.querySelectorAll('g[data-selected]').length).toBe(0)
})
