import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getCalibration, getParameterErrors, parameterFields } from './parameterValidation.js'
import styles from './ParameterStep.module.css'

const calibrationField = parameterFields[0]
const analysisFields = parameterFields.slice(1)

function ParameterField({ field, error, onBlur, onChange, value }) {
  const { t } = useTranslation()
  const fieldId = `parameter-${field.name}`
  const helpId = `${fieldId}-help`
  const errorId = `${fieldId}-error`
  const fieldLabel = t(`${field.translationKey}.label`)
  const describedBy = error ? `${helpId} ${errorId}` : helpId

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={fieldId}>{fieldLabel}</label>
      <p id={helpId} className={styles.helpText}>{t(`${field.translationKey}.help`)}</p>
      <div className={styles.inputGroup}>
        <input
          id={fieldId}
          className={styles.input}
          name={field.name}
          type="number"
          inputMode="decimal"
          min={field.minimum}
          max={field.maximum}
          step={field.step}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          aria-describedby={describedBy}
          aria-invalid={Boolean(error)}
        />
        <span className={styles.unit} aria-hidden="true">{t(`${field.translationKey}.unit`)}</span>
      </div>
      {error && (
        <p id={errorId} className={styles.error} role="alert">
          {t(`wizard.parameters.errors.${error}`, {
            field: fieldLabel,
            minimum: field.minimum,
            maximum: field.maximum,
          })}
        </p>
      )}
    </div>
  )
}

function ParameterStep({ parameters, points, updateWizardData }) {
  const { t } = useTranslation()
  const [touchedFields, setTouchedFields] = useState({})
  const errors = getParameterErrors(parameters)
  const calibration = getCalibration(points, parameters.eyeToEyeMm)

  function handleChange(event) {
    const { name, value } = event.target

    updateWizardData({
      parameters: {
        ...parameters,
        [name]: value,
      },
    })
  }

  function handleBlur(event) {
    setTouchedFields((currentFields) => ({
      ...currentFields,
      [event.target.name]: true,
    }))
  }

  function getVisibleError(fieldName) {
    return touchedFields[fieldName] ? errors[fieldName] : undefined
  }

  return (
    <div className={styles.content}>
      <div>
        <p className={styles.eyebrow}>{t('wizard.parameters.eyebrow')}</p>
        <h1 id="wizard-title" className={styles.title}>{t('wizard.parameters.title')}</h1>
        <p className={styles.description}>{t('wizard.parameters.description')}</p>
      </div>

      <section className={styles.section} aria-labelledby="calibration-heading">
        <div>
          <h2 id="calibration-heading" className={styles.sectionTitle}>{t('wizard.parameters.calibration.title')}</h2>
          <p className={styles.sectionDescription}>{t('wizard.parameters.calibration.description')}</p>
        </div>
        <ParameterField
          field={calibrationField}
          value={parameters[calibrationField.name]}
          error={getVisibleError(calibrationField.name)}
          onChange={handleChange}
          onBlur={handleBlur}
        />

        {!calibration && (
          <p className={styles.calibrationHint}>{t('wizard.parameters.calibration.pending')}</p>
        )}
        {calibration && !calibration.isValid && (
          <p className={styles.error} role="alert">{t('wizard.parameters.calibration.invalidReference')}</p>
        )}
        {calibration?.isValid && (
          <div className={styles.scaleResult} data-warning={calibration.isHighScale}>
            <p className={styles.scaleValue}>
              {t('wizard.parameters.calibration.scaleValue', {
                value: calibration.mmPerPixel.toFixed(2),
              })}
            </p>
            <p className={styles.scaleExplanation}>
              {t(calibration.isHighScale
                ? 'wizard.parameters.calibration.highScaleWarning'
                : 'wizard.parameters.calibration.usableScale')}
            </p>
          </div>
        )}
      </section>

      <section className={styles.section} aria-labelledby="analysis-parameters-heading">
        <div>
          <h2 id="analysis-parameters-heading" className={styles.sectionTitle}>{t('wizard.parameters.analysis.title')}</h2>
          <p className={styles.sectionDescription}>{t('wizard.parameters.analysis.description')}</p>
        </div>
        <div className={styles.fieldGrid}>
          {analysisFields.map((field) => (
            <ParameterField
              key={field.name}
              field={field}
              value={parameters[field.name]}
              error={getVisibleError(field.name)}
              onChange={handleChange}
              onBlur={handleBlur}
            />
          ))}
        </div>
      </section>
    </div>
  )
}

export default ParameterStep
