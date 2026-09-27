import { expect, it } from 'vitest'
import { chartAxisSize, chartMargin, getEqualScaleDomains, getFittedAxis, relativeAxlePath } from './chartGeometry.js'

it('uses the starting axle position as zero and upwards as positive', () => {
  expect(relativeAxlePath([{ x: 100, y: 300 }, { x: 90, y: 200 }, { x: 110, y: 100 }]))
    .toEqual([{ x: 0, y: 0 }, { x: -10, y: 100 }, { x: 10, y: 200 }])
})

it.each([248, 320, 544])('preserves mm per pixel on a square chart %s px wide', (size) => {
  const domains = getEqualScaleDomains([{ x: 0, y: 0 }, { x: -10, y: 164 }])
  const width = size - chartMargin.left - chartMargin.right - chartAxisSize
  const height = size - chartMargin.top - chartMargin.bottom - chartAxisSize
  expect(width / (domains.xDomain[1] - domains.xDomain[0]))
    .toBeCloseTo(height / (domains.yDomain[1] - domains.yDomain[0]), 10)
})

it('fits the leverage axis to the curve with round ticks instead of starting at zero', () => {
  expect(getFittedAxis([3.1, 2.7, 2.25, 2.29])).toEqual({
    yDomain: [2, 3.25],
    yTicks: [2, 2.25, 2.5, 2.75, 3, 3.25],
  })
})

it('keeps a minimum span so an almost flat curve still looks flat', () => {
  const { yDomain, yTicks } = getFittedAxis([2.6, 2.61, 2.62])
  expect(yDomain[1] - yDomain[0]).toBeGreaterThanOrEqual(0.5)
  expect(yDomain[0]).toBeLessThan(2.6)
  expect(yDomain[1]).toBeGreaterThan(2.62)
  expect(yTicks.length).toBeGreaterThanOrEqual(3)
})
