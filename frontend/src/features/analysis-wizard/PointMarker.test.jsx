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

it('zooms with a pinch, pans with the right button without marking, and marks the pedalier second', () => {
  render(<MarkerTest />)
  const marker = screen.getByRole('application')
  vi.spyOn(marker, 'getBoundingClientRect').mockReturnValue({ left: 0, top: 0, width: 600, height: 400 })
  fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaY: -50, ctrlKey: true })
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

function renderMeasuredMarker() {
  render(<MarkerTest />)
  const marker = screen.getByRole('application')
  vi.spyOn(marker, 'getBoundingClientRect').mockReturnValue({ left: 0, top: 0, width: 600, height: 400 })
  const box = () => marker.getAttribute('viewBox').split(' ').map(Number)
  return { marker, box }
}

it('lets a two-finger swipe scroll the page until the photo is zoomed in, then moves the photo', () => {
  const { marker, box } = renderMeasuredMarker()
  expect(fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaX: 60, deltaY: 40 })).toBe(true)
  expect(box()).toEqual([0, 0, 1800, 1200])

  expect(fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaY: -50, ctrlKey: true })).toBe(false)
  const [x, y, width, height] = box()
  expect(width).toBeLessThan(1800)

  expect(fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaX: 60, deltaY: 40 })).toBe(false)
  const [movedX, movedY, movedWidth, movedHeight] = box()
  expect([movedWidth, movedHeight]).toEqual([width, height])
  expect(movedX).toBeCloseTo(x + 60 / 600 * width)
  expect(movedY).toBeCloseTo(y + 40 / 400 * height)
  expect(JSON.parse(screen.getByLabelText('Stored marks').textContent)).toEqual({})
})

it('zooms with the Safari pinch gesture and ignores its duplicate wheel events meanwhile', () => {
  const { marker, box } = renderMeasuredMarker()
  const gesture = (type, scale) => fireEvent(marker,
    Object.assign(new Event(type, { cancelable: true }), { scale, clientX: 300, clientY: 200 }))

  expect(gesture('gesturestart', 1)).toBe(false)
  gesture('gesturechange', 2)
  expect(box()[2]).toBeCloseTo(900)
  expect(fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaY: -50, ctrlKey: true })).toBe(false)
  expect(box()[2]).toBeCloseTo(900)
  gesture('gesturechange', 3)
  expect(box()[2]).toBeCloseTo(600)
  gesture('gestureend', 3)

  fireEvent.wheel(marker, { clientX: 300, clientY: 200, deltaY: 50, ctrlKey: true })
  expect(box()[2]).toBeGreaterThan(600)
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

it('switches to the ten-point rigid-yoke Horst link guide', () => {
  const { container } = render(<MarkerTest suspensionLayout="HORST_LINK_YOKE" />)
  const marker = screen.getByRole('application', { name: 'Bike photo used to mark suspension points' })

  for (let index = 0; index < 10; index++) {
    fireEvent.keyDown(marker, { key: 'ArrowRight', shiftKey: true })
    fireEvent.keyDown(marker, { key: 'Enter' })
  }

  expect(screen.getByText('10 of 10 points marked')).toBeTruthy()
  expect(container.querySelectorAll('g[data-selected]').length).toBe(10)
  const marks = JSON.parse(screen.getByLabelText('Stored marks').textContent)
  expect(marks.YOKE_ROCKER_PIVOT).toBeTruthy()
  expect(marks.SHOCK_YOKE_EYE).toBeTruthy()
})

it('asks for the shock mount on the seatstay instead of the rocker in the seatstay-driven guide', () => {
  render(<MarkerTest suspensionLayout="HORST_LINK_SEATSTAY" />)
  const marker = screen.getByRole('application', { name: 'Bike photo used to mark suspension points' })

  for (let index = 0; index < 9; index++) {
    fireEvent.keyDown(marker, { key: 'ArrowRight', shiftKey: true })
    fireEvent.keyDown(marker, { key: 'Enter' })
  }

  expect(screen.getByText('9 of 9 points marked')).toBeTruthy()
  const marks = JSON.parse(screen.getByLabelText('Stored marks').textContent)
  expect(marks.SHOCK_SEATSTAY).toBeTruthy()
  expect(marks.SHOCK_ROCKER).toBeUndefined()
})
