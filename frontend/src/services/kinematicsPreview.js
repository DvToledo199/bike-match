import { referenceSetupFor } from '../models/bikeTypes.js'
import { requestApi } from './apiClient.js'
import { validatePreview } from './previewValidation.js'

function toNumber(value) {
  return Number(value)
}

export async function requestKinematicsPreview(wizardData, signal) {
  const { parameters, points } = wizardData
  const setup = referenceSetupFor(parameters.bikeType)

  const response = await requestApi('/api/kinematics/preview', {
    signal,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      points: Object.values(points).map(({ type, x, y }) => ({ type, x, y })),
      suspensionLayout: wizardData.suspensionLayout,
      eyeToEyeMm: toNumber(parameters.eyeToEyeMm),
      parameters: {
        shockStrokeMm: toNumber(parameters.shockStrokeMm),
        chainringTeeth: setup.chainringTeeth,
        sprocketTeeth: setup.sprocketTeeth,
        declaredTravelMm: toNumber(parameters.declaredTravelMm),
        sagPercent: setup.sagPercent,
        wheelConfiguration: parameters.wheelConfiguration,
      },
    }),
  })
  return validatePreview(response)
}
