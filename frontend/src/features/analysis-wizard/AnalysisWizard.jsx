import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import useWizardState from './useWizardState.js'
import usePreview from './usePreview.js'
import ParameterStep from './ParameterStep.jsx'
import PointMarker from './PointMarker.jsx'
import PhotoStep from './PhotoStep.jsx'
import PreviewStatus from './PreviewStatus.jsx'
import SaveAnalysisPanel from './SaveAnalysisPanel.jsx'
import { isStepComplete, wizardSteps } from './wizardSteps.js'
import styles from './AnalysisWizard.module.css'

function AnalysisWizard({ active = true, session = null, onSaved = () => {} }) {
  const { t } = useTranslation()
  const {
    activeStepIndex,
    wizardData,
    updateWizardData,
    goToNextStep,
    goToPreviousStep,
    resetWizard,
  } = useWizardState(wizardSteps.length)
  const { data, error, isLoading, requestPreview, cancelPreview } = usePreview()
  const panelRef = useRef(null)
  const previousStep = useRef(activeStepIndex)
  const [saveLocked, setSaveLocked] = useState(false)
  const [saveKey, setSaveKey] = useState(0)

  function startNewAnalysis() {
    cancelPreview()
    resetWizard()
    setSaveLocked(false)
    setSaveKey((key) => key + 1)
  }

  useEffect(() => {
    if (active && previousStep.current !== activeStepIndex) {
      panelRef.current?.focus()
      previousStep.current = activeStepIndex
    }
  }, [activeStepIndex, active])

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

  function handleSuspensionLayoutChange(suspensionLayout) {
    cancelPreview()
    updateWizardData({ suspensionLayout, points: {} })
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

      <div className={styles.panel} ref={panelRef} tabIndex={-1} aria-labelledby="wizard-title">
        <div id="save-analysis-panel" className={styles.savePanel} tabIndex={-1} hidden={!isLastStep || !data || isLoading || Boolean(error)}>
          <SaveAnalysisPanel key={saveKey} session={session} wizardData={wizardData}
            onLockedChange={setSaveLocked} onSaved={onSaved} onStartNew={startNewAnalysis} />
        </div>
        {activeStep.id === 'photo' ? (
          <PhotoStep
            photo={wizardData.photo}
            suspensionLayout={wizardData.suspensionLayout}
            updateWizardData={updateWizardData}
            onSuspensionLayoutChange={handleSuspensionLayoutChange}
          />
        ) : activeStep.id === 'marking' ? (
          <PointMarker
            photo={wizardData.photo}
            points={wizardData.points}
            suspensionLayout={wizardData.suspensionLayout}
            updateWizardData={updateWizardData}
          />
        ) : activeStep.id === 'parameters' ? (
          <ParameterStep
            parameters={wizardData.parameters}
            points={wizardData.points}
            suspensionLayout={wizardData.suspensionLayout}
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
        {isLastStep && data && !isLoading && !error && (
          <section className={styles.saveReminder} aria-label={t('saveAnalysis.resultsActionLabel')}>
            <p>{t('saveAnalysis.reminder')}</p>
            <div className={styles.saveActions}>
              {session ? (
                <button type="button" className={styles.primaryButton} onClick={() => {
                  const panel = document.getElementById('save-analysis-panel')
                  panel?.focus({ preventScroll: true })
                  panel?.scrollIntoView({ block: 'start' })
                }}>{t(saveLocked ? 'saveAnalysis.reviewSave' : 'saveAnalysis.save')}</button>
              ) : (
                <>
                  <a className={styles.primaryButton} href="#/register">{t('saveAnalysis.registerToSave')}</a>
                  <a className={styles.secondaryButton} href="#/login">{t('saveAnalysis.loginToSave')}</a>
                </>
              )}
            </div>
          </section>
        )}
      </div>

      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => { cancelPreview(); goToPreviousStep() }}
          disabled={isFirstStep || saveLocked}
        >
          {t('wizard.back')}
        </button>
        {!isLastStep && (
          <button
            type="button"
            className={styles.primaryButton}
            onClick={handleNextStep}
            disabled={!canGoToNextStep || isLoading}
          >
            {activeStep.id === 'parameters' ? t('wizard.calculate') : t('wizard.next')}
          </button>
        )}
      </div>
    </section>
  )
}

export default AnalysisWizard
