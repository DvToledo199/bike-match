import { useState } from 'react'

const initialWizardData = {
  photo: null,
  suspensionType: null,
  points: {},
  parameters: {
    eyeToEyeMm: '',
    shockStrokeMm: '',
    chainringTeeth: '',
    sprocketTeeth: '',
    declaredTravelMm: '',
    sagPercent: '',
  },
}

function useWizardState(totalSteps) {
  const [activeStepIndex, setActiveStepIndex] = useState(0)
  const [wizardData, setWizardData] = useState(initialWizardData)

  function updateWizardData(partialData) {
    setWizardData((currentData) => ({
      ...currentData,
      ...partialData,
    }))
  }

  function goToPreviousStep() {
    setActiveStepIndex((currentIndex) => Math.max(0, currentIndex - 1))
  }

  function goToNextStep() {
    setActiveStepIndex((currentIndex) => Math.min(totalSteps - 1, currentIndex + 1))
  }

  return {
    activeStepIndex,
    wizardData,
    updateWizardData,
    goToPreviousStep,
    goToNextStep,
  }
}

export default useWizardState

