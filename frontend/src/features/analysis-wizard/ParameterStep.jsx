import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getCalibration, getParameterErrors, parameterFields } from './parameterValidation.js'
import { wheelConfigurations } from '../../models/wheelConfigurations.js'
import styles from './ParameterStep.module.css'

function ParameterField({ field, error, onBlur, onChange, value }) {
  const { t } = useTranslation()
  const fieldId = `parameter-${field.name}`
  const helpId = `${fieldId}-help`
  const errorId = `${fieldId}-error`
  const fieldLabel = t(`${field.translationKey}.label`)
  const unitId = `${fieldId}-unit`
  const describedBy = error ? `${helpId} ${unitId} ${errorId}` : `${helpId} ${unitId}`

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
          inputMode={field.step === 1 ? "numeric" : "decimal"}
          min={field.minimum}
          max={field.maximum}
          step={field.step}
          value={value ?? ''}
          onChange={onChange}
          onBlur={onBlur}
          aria-describedby={describedBy}
          aria-invalid={Boolean(error)}
        />
        <span id={unitId} className={styles.unit}>{t(`${field.translationKey}.unit`)}</span>
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

      <div className={styles.fieldGrid}>
          {parameterFields.map((field) => (
            <ParameterField key={field.name} field={field} value={parameters[field.name]}
              error={getVisibleError(field.name)} onChange={handleChange} onBlur={handleBlur} />
          ))}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="parameter-wheelConfiguration">{t('wizard.parameters.wheels.label')}</label>
            <p className={styles.helpText} id="wheel-help">{t('wizard.parameters.wheels.help')}</p>
            <select
              id="parameter-wheelConfiguration"
              name="wheelConfiguration"
              className={`${styles.input} ${styles.select}`}
              value={parameters.wheelConfiguration ?? ''}
              onChange={handleChange}
              onBlur={handleBlur}
              required
              aria-invalid={Boolean(getVisibleError('wheelConfiguration'))}
              aria-describedby={getVisibleError('wheelConfiguration') ? 'wheel-help wheel-error' : 'wheel-help'}
            >
              <option value="" disabled>{t('wizard.parameters.wheels.placeholder')}</option>
              {wheelConfigurations.map((configuration) => (
                <option key={configuration} value={configuration}>{t(`wizard.parameters.wheels.options.${configuration}`)}</option>
              ))}
            </select>
            {getVisibleError('wheelConfiguration') && (
              <p id="wheel-error" className={styles.error} role="alert">{t('wizard.parameters.errors.wheelConfiguration')}</p>
            )}
          </div>
      </div>
      {calibration && !calibration.isValid && (
        <p className={styles.error} role="alert">{t('wizard.parameters.calibration.invalidReference')}</p>
      )}
      {calibration?.isValid && calibration.isHighScale && (
        <p className={styles.scaleWarning} role="status">{t('wizard.parameters.calibration.highScaleWarning')}</p>
      )}
    </div>
  )
}

export default ParameterStep
