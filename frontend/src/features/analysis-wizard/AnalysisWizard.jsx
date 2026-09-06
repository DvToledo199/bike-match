import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import useWizardState from './useWizardState.js'
import usePreview from './usePreview.js'
import ParameterStep from './ParameterStep.jsx'
import PointMarker from './PointMarker.jsx'
import PhotoStep from './PhotoStep.jsx'
import PreviewStatus from './PreviewStatus.jsx'
import { isStepComplete, wizardSteps } from './wizardSteps.js'
import styles from './AnalysisWizard.module.css'

function AnalysisWizard() {
  const { t } = useTranslation()
  const {
    activeStepIndex,
    wizardData,
    updateWizardData,
    goToNextStep,
    goToPreviousStep,
  } = useWizardState(wizardSteps.length)
  const { data, error, isLoading, requestPreview, cancelPreview } = usePreview()

  useEffect(() => {
    return () => {
      if (wizardData.photo?.previewUrl) {
        URL.revokeObjectURL(wizardData.photo.previewUrl)
      }
    }
  }, [wizardData.photo?.previewUrl])

  const activeStep = wizardSteps[activeStepIndex]
  const isFirstStep = activeStepIndex === 0
  const isLastStep = activeStepIndex === wizardSteps.length - 1
  const canGoToNextStep = isStepComplete(activeStep.id, wizardData)

  async function handleNextStep() {
    if (activeStep.id === 'parameters') {
      goToNextStep()
      await requestPreview(wizardData)
      return
    }

    goToNextStep()
  }

  return (
    <section className={styles.wizard} aria-labelledby="wizard-title">
      <nav aria-label={t('wizard.progressLabel')}>
        <ol className={styles.steps}>
          {wizardSteps.map((step, index) => {
            const isActive = index === activeStepIndex
            const isComplete = index < activeStepIndex

            return (
              <li
                key={step.id}
                className={styles.step}
                aria-current={isActive ? 'step' : undefined}
              >
                <span className={`${styles.stepNumber} ${isActive ? styles.active : ''} ${isComplete ? styles.complete : ''}`}>
                  {index + 1}
                </span>
                <span className={styles.stepLabel}>{t(`${step.translationKey}.label`)}</span>
              </li>
            )
          })}
        </ol>
      </nav>

      <div className={styles.panel}>
        {activeStep.id === 'photo' ? (
          <PhotoStep
            photo={wizardData.photo}
            suspensionType={wizardData.suspensionType}
            updateWizardData={updateWizardData}
          />
        ) : activeStep.id === 'marking' ? (
          <PointMarker
            photo={wizardData.photo}
            points={wizardData.points}
            updateWizardData={updateWizardData}
          />
        ) : activeStep.id === 'parameters' ? (
          <ParameterStep
            parameters={wizardData.parameters}
            points={wizardData.points}
            updateWizardData={updateWizardData}
          />
        ) : activeStep.id === 'results' ? (
          <PreviewStatus
            data={data}
            error={error}
            isLoading={isLoading}
            onRetry={() => requestPreview(wizardData)}
          />
        ) : (
          <>
            <p className={styles.stepCount}>
              {t('wizard.stepCount', {
                current: activeStepIndex + 1,
                total: wizardSteps.length,
              })}
            </p>
            <h1 id="wizard-title" className={styles.title}>
              {t(`${activeStep.translationKey}.title`)}
            </h1>
            <p className={styles.description}>{t(`${activeStep.translationKey}.description`)}</p>
            <p className={styles.placeholder}>{t('wizard.placeholder')}</p>
          </>
        )}
      </div>

      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => { cancelPreview(); goToPreviousStep() }}
          disabled={isFirstStep}
        >
          {t('wizard.back')}
        </button>
        <button
          type="button"
          className={styles.primaryButton}
          onClick={handleNextStep}
          disabled={isLastStep || !canGoToNextStep || isLoading}
        >
          {activeStep.id === 'parameters'
            ? t('wizard.calculate')
            : isLastStep ? t('wizard.finish') : t('wizard.next')}
        </button>
      </div>
    </section>
  )
}

export default AnalysisWizard
