import { expect, it } from 'vitest'
import { chartAxisSize, chartMargin, getEqualScaleDomains, getKickbackLevel, relativeAxlePath } from './chartGeometry.js'

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

it('does not confuse negative chain release with backwards pedal kick', () => {
  expect(getKickbackLevel([{ kickbackDegrees: -60 }, { kickbackDegrees: 12 }])).toBe('low')
  expect(getKickbackLevel([{ kickbackDegrees: 0 }, { kickbackDegrees: 35 }])).toBe('high')
})
