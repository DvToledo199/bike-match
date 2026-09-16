import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import PointMarker from './PointMarker.jsx'

beforeEach(() => {
  vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
  vi.stubGlobal('PointerEvent', MouseEvent)
})

function MarkerTest({ suspensionLayout = 'SINGLE_PIVOT' }) {
  const [points, setPoints] = useState({})
  return <><PointMarker photo={{ width: 1800, height: 1200, previewUrl: 'blob:photo' }} points={points}
    suspensionLayout={suspensionLayout} updateWizardData={(update) => setPoints(update.points)} />
    <output aria-label="Stored marks">{JSON.stringify(points)}</output></>
}

it('zooms with the wheel, pans with the right button without marking, and marks the pedalier second', () => {
  render(<MarkerTest />)
  const marker = screen.getByRole('application')
  vi.spyOn(marker, 'getBoundingClientRect').mockReturnValue({ left: 0, top: 0, width: 600, height: 400 })
  fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaY: -200 })
  const boxBefore = marker.getAttribute('viewBox')
  fireEvent.pointerDown(marker, { button: 2, clientX: 300, clientY: 200 })
  fireEvent.pointerMove(marker, { buttons: 2, clientX: 350, clientY: 230 })
  fireEvent.pointerUp(marker, { button: 2, clientX: 350, clientY: 230 })
  expect(marker.getAttribute('viewBox')).not.toBe(boxBefore)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent)).toEqual({})
  fireEvent.pointerDown(marker, { button: 0, clientX: 300, clientY: 200 })
  fireEvent.pointerUp(marker, { button: 0, clientX: 300, clientY: 200 })
  expect(screen.getByRole('heading', { name: 'Bottom bracket' })).toBeTruthy()
  const first = JSON.parse(screen.getByLabelText('Stored marks').textContent).MAIN_PIVOT
  fireEvent.keyDown(marker, { key: 'Enter' })
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent).MAIN_PIVOT).toEqual(first)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent).BOTTOM_BRACKET).toBeTruthy()
})

it('creates all six points with the keyboard, and undo removes both cross and data immediately', () => {
  const { container } = render(<MarkerTest />)
  const marker = screen.getByRole('application', { name: 'Bike photo used to mark suspension points' })
  for (let i = 0; i < 6; i++) {
    fireEvent.keyDown(marker, { key: 'ArrowRight', shiftKey: true })
    fireEvent.keyDown(marker, { key: 'Enter' })
  }
  expect(screen.getByText('6 of 6 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(6)
  fireEvent.click(screen.getByRole('button', { name: /Undo point/ }))
  expect(screen.getByText('5 of 6 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(5)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent).FRONT_AXLE).toBeUndefined()
  for (let i = 0; i < 5; i++) fireEvent.click(screen.getByRole('button', { name: /Undo point/ }))
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent)).toEqual({})
  expect(container.querySelectorAll('g[data-selected]').length).toBe(0)
})

it('switches to the nine-point Horst link guide', () => {
  const { container } = render(<MarkerTest suspensionLayout="HORST_LINK" />)
  const marker = screen.getByRole('application', { name: 'Bike photo used to mark suspension points' })
  expect(screen.getByRole('heading', { name: 'Main pivot' })).toBeTruthy()

  for (let index = 0; index < 9; index++) {
    fireEvent.keyDown(marker, { key: 'ArrowRight', shiftKey: true })
    fireEvent.keyDown(marker, { key: 'Enter' })
  }

  expect(screen.getByText('9 of 9 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(9)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent).SHOCK_ROCKER).toBeTruthy()
})
