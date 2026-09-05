import { hasAllPoints } from './pointDefinitions.js'

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

  // The remaining steps are placeholders until their own implementation issues.
  // The parameter validation rule will be added with #49.
  return true
}
