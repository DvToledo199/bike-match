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
    minimum: 20,
    maximum: 60,
    step: '1',
    translationKey: 'wizard.parameters.fields.chainring',
  },
  {
    name: 'sprocketTeeth',
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
  return parameterFields.reduce((errors, field) => {
    const value = parameters[field.name]
    const parsedValue = Number(value)

    if (value === '' || value === null || value === undefined) {
      errors[field.name] = 'required'
    } else if (!Number.isFinite(parsedValue) || parsedValue < field.minimum || parsedValue > field.maximum) {
      errors[field.name] = 'range'
    }

    return errors
  }, {})
}

export function hasValidParameters(parameters) {
  return Object.keys(getParameterErrors(parameters)).length === 0
}

export function getCalibration(points, eyeToEyeMm) {
  const eyeToEye = Number(eyeToEyeMm)
  const shockFrame = points.SHOCK_FRAME
  const shockSwingarm = points.SHOCK_SWINGARM

  if (!Number.isFinite(eyeToEye) || eyeToEye <= 0 || !shockFrame || !shockSwingarm) {
    return null
  }

  const referenceDistancePixels = Math.hypot(
    shockFrame.x - shockSwingarm.x,
    shockFrame.y - shockSwingarm.y,
  )

  if (referenceDistancePixels < 0.01) {
    return { isValid: false }
  }

  const mmPerPixel = eyeToEye / referenceDistancePixels

  return {
    isValid: true,
    mmPerPixel,
    isHighScale: mmPerPixel > 2,
  }
}
