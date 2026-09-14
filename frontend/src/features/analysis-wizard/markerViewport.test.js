import { expect, it } from 'vitest'
import { constrainView, getViewBox, imagePoint, zoomAt } from './markerViewport.js'

const photo = { width: 1800, height: 1200 }
const view = { zoom: 2, center: { x: 900, y: 600 } }
it('keeps the image coordinate under the mouse fixed when zooming away from the edges', () => {
  const bounds = { left: 0, top: 0, width: 600, height: 400 }
  const before = imagePoint(getViewBox(photo, 2, view.center), bounds, 200, 100)
  const next = zoomAt(photo, view, 6, { x: 1 / 3, y: 1 / 4 }, 1.5)
  const after = imagePoint(getViewBox(photo, next.zoom, next.center), bounds, 200, 100)
  expect(after.x).toBeCloseTo(before.x)
  expect(after.y).toBeCloseTo(before.y)
})
it('fits portrait photos without distortion and constrains pan and zoom', () => {
  const portrait = { width: 600, height: 1200 }
  const box = getViewBox(portrait, 1, { x: 300, y: 600 }, 2)
  expect(box.width / box.height).toBe(2)
  expect(box).toEqual({ x: -900, y: 0, width: 2400, height: 1200 })
  expect(zoomAt(photo, view, 30, null, 1.5).zoom).toBe(12)
  expect(zoomAt(photo, view, 0.1, null, 1.5).zoom).toBe(1)
  expect(constrainView(photo, { zoom: 2, center: { x: -500, y: 4000 } }, 1.5).center).toEqual({ x: 450, y: 900 })
})
