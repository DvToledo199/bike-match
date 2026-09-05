import { hasAllPoints } from './pointDefinitions.js'
import { getCalibration, hasValidParameters } from './parameterValidation.js'

export const wizardSteps = [
  {
    id: 'photo',
    translationKey: 'wizard.photo',
  },
  {
    id: 'marking',
    translationKey: 'wizard.marking',
  },
  {
    id: 'parameters',
    translationKey: 'wizard.parameters',
  },
  {
    id: 'results',
    translationKey: 'wizard.results',
  },
]

export function isStepComplete(stepId, wizardData) {
  if (stepId === 'photo') {
    return Boolean(wizardData.photo && wizardData.suspensionType)
  }

  if (stepId === 'marking') {
    return hasAllPoints(wizardData.points)
  }

  if (stepId === 'parameters') {
    return hasValidParameters(wizardData.parameters)
      && getCalibration(wizardData.points, wizardData.parameters.eyeToEyeMm)?.isValid
  }

  // The results step is a placeholder until its own implementation issue.
  return true
}
