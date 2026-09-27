import { expect, it } from 'vitest'
import { getCalibration, getParameterErrors, hasValidParameters } from './parameterValidation.js'

const valid = { bikeType: 'ENDURO', eyeToEyeMm: '230', shockStrokeMm: '65', declaredTravelMm: '164', wheelConfiguration: 'MULLET' }
it('accepts valid measurements and rejects them out of range', () => {
  expect(hasValidParameters(valid)).toBe(true)
  expect(getParameterErrors({ ...valid, shockStrokeMm: '10', declaredTravelMm: '' }))
    .toEqual({ shockStrokeMm: 'range', declaredTravelMm: 'required' })
})
/** The type replaces the chainring, sprocket and sag questions, so it has to be one we know. */
it('requires a supported bike type', () => {
  for (const bikeType of ['', undefined, 'XC', 0]) {
    expect(getParameterErrors({ ...valid, bikeType }).bikeType).toBe('bikeType')
  }
  for (const bikeType of ['ENDURO', 'E_ENDURO', 'DOWNHILL']) {
    expect(hasValidParameters({ ...valid, bikeType })).toBe(true)
  }
})
it('requires an explicit supported wheel choice', () => {
  for (const wheelConfiguration of ['', undefined, 'FULL_26', 0]) {
    expect(getParameterErrors({ ...valid, wheelConfiguration }).wheelConfiguration).toBe('wheelConfiguration')
  }
  for (const wheelConfiguration of ['FULL_29', 'MULLET', 'FULL_27_5']) {
    expect(hasValidParameters({ ...valid, wheelConfiguration })).toBe(true)
  }
})
it('rejects degenerate and non-finite calibration', () => {
  expect(getCalibration({ SHOCK_FRAME: { x: 2, y: 2 }, SHOCK_SWINGARM: { x: 2, y: 2 } }, 230).isValid).toBe(false)
  expect(getCalibration({ SHOCK_FRAME: { x: NaN, y: 2 }, SHOCK_SWINGARM: { x: 3, y: 2 } }, 230).isValid).toBe(false)
})

it('uses the rocker-side shock eye to calibrate a Horst link', () => {
  const points = {
    SHOCK_FRAME: { x: 10, y: 10 },
    SHOCK_ROCKER: { x: 110, y: 10 },
    SHOCK_SWINGARM: { x: 10, y: 10 },
  }

  expect(getCalibration(points, 200, 'HORST_LINK')).toMatchObject({ isValid: true, mmPerPixel: 2 })
})

it('uses the physical shock eye instead of the yoke pivot to calibrate a yoke Horst link', () => {
  const points = {
    SHOCK_FRAME: { x: 10, y: 10 },
    YOKE_ROCKER_PIVOT: { x: 60, y: 10 },
    SHOCK_YOKE_EYE: { x: 110, y: 10 },
  }

  expect(getCalibration(points, 200, 'HORST_LINK_YOKE')).toMatchObject({ isValid: true, mmPerPixel: 2 })
})
