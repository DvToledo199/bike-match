import { useTranslation } from 'react-i18next'
import useWizardState from './useWizardState.js'
import { wizardSteps } from './wizardSteps.js'
import styles from './AnalysisWizard.module.css'

function AnalysisWizard() {
  const { t } = useTranslation()
  const {
    activeStepIndex,
    goToNextStep,
    goToPreviousStep,
  } = useWizardState(wizardSteps.length)

  const activeStep = wizardSteps[activeStepIndex]
  const isFirstStep = activeStepIndex === 0
  const isLastStep = activeStepIndex === wizardSteps.length - 1

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
      </div>

      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={goToPreviousStep}
          disabled={isFirstStep}
        >
          {t('wizard.back')}
        </button>
        <button
          type="button"
          className={styles.primaryButton}
          onClick={goToNextStep}
          disabled={isLastStep}
        >
          {isLastStep ? t('wizard.finish') : t('wizard.next')}
        </button>
      </div>
    </section>
  )
}

export default AnalysisWizard

