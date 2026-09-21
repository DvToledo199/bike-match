import { wheelConfigurations } from '../../models/wheelConfigurations.js'

export const parameterFields = [
  {
    name: 'eyeToEyeMm',
    minimum: 100,
    maximum: 300,
    step: '0.5',
    translationKey: 'wizard.parameters.fields.eyeToEye',
  },
  {
    name: 'shockStrokeMm',
    minimum: 20,
    maximum: 120,
    step: '0.5',
    translationKey: 'wizard.parameters.fields.shockStroke',
  },
  {
    name: 'chainringTeeth',
    integer: true,
    minimum: 28,
    maximum: 38,
    step: '1',
    translationKey: 'wizard.parameters.fields.chainring',
  },
  {
    name: 'sprocketTeeth',
    integer: true,
    minimum: 10,
    maximum: 60,
    step: '1',
    translationKey: 'wizard.parameters.fields.sprocket',
  },
  {
    name: 'declaredTravelMm',
    minimum: 50,
    maximum: 250,
    step: '1',
    translationKey: 'wizard.parameters.fields.declaredTravel',
  },
  {
    name: 'sagPercent',
    minimum: 10,
    maximum: 50,
    step: '1',
    translationKey: 'wizard.parameters.fields.sag',
  },
]

export function getParameterErrors(parameters) {
  const errors = parameterFields.reduce((errors, field) => {
    const value = parameters[field.name]
    const parsedValue = Number(value)

    if (value === '' || value === null || value === undefined) {
      errors[field.name] = 'required'
    } else if (!Number.isFinite(parsedValue) || parsedValue < field.minimum || parsedValue > field.maximum) {
      errors[field.name] = 'range'
    } else if (field.integer && !Number.isInteger(parsedValue)) {
      errors[field.name] = 'integer'
    }

    return errors
  }, {})
  if (!wheelConfigurations.includes(parameters.wheelConfiguration)) errors.wheelConfiguration = 'wheelConfiguration'
  return errors
}

export function hasValidParameters(parameters) {
  return Object.keys(getParameterErrors(parameters)).length === 0
}

export function getCalibration(points, eyeToEyeMm, suspensionLayout = 'SINGLE_PIVOT') {
  const eyeToEye = Number(eyeToEyeMm)
  const shockFrame = points.SHOCK_FRAME
  const movingShockEye = {
    SINGLE_PIVOT: points.SHOCK_SWINGARM,
    HORST_LINK: points.SHOCK_ROCKER,
    HORST_LINK_YOKE: points.SHOCK_YOKE_EYE,
  }[suspensionLayout]

  if (!Number.isFinite(eyeToEye) || eyeToEye <= 0 || !shockFrame || !movingShockEye) {
    return null
  }

  const referenceDistancePixels = Math.hypot(
    shockFrame.x - movingShockEye.x,
    shockFrame.y - movingShockEye.y,
  )

  if (!Number.isFinite(referenceDistancePixels) || referenceDistancePixels < 0.01) {
    return { isValid: false }
  }

  const mmPerPixel = eyeToEye / referenceDistancePixels

  return {
    isValid: true,
    mmPerPixel,
    isHighScale: mmPerPixel > 2,
  }
}
