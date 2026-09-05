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

  // The remaining steps are placeholders until their own implementation issues.
  // Their real validation rules will be added with #47 and #49.
  return true
}
