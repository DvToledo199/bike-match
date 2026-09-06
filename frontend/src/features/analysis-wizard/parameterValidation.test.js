import { expect, it } from 'vitest'
import { getCalibration, getParameterErrors, hasValidParameters } from './parameterValidation.js'

const valid = { eyeToEyeMm: '230', shockStrokeMm: '65', chainringTeeth: '34', sprocketTeeth: '50', declaredTravelMm: '164', sagPercent: '30' }
it('accepts valid measurements but not fractional teeth', () => {
  expect(hasValidParameters(valid)).toBe(true)
  expect(getParameterErrors({ ...valid, chainringTeeth: '34.9', sprocketTeeth: '10.5' }))
    .toEqual({ chainringTeeth: 'integer', sprocketTeeth: 'integer' })
})
it('rejects degenerate and non-finite calibration', () => {
  expect(getCalibration({ SHOCK_FRAME: { x: 2, y: 2 }, SHOCK_SWINGARM: { x: 2, y: 2 } }, 230).isValid).toBe(false)
  expect(getCalibration({ SHOCK_FRAME: { x: NaN, y: 2 }, SHOCK_SWINGARM: { x: 3, y: 2 } }, 230).isValid).toBe(false)
})
