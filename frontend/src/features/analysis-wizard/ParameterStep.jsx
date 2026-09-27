import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getCalibration, getParameterErrors, parameterFields } from './parameterValidation.js'
import { bikeTypeNames, bikeTypes } from '../../models/bikeTypes.js'
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

/** The bike type as cards: the name stands out and the gearing it stands for sits below in small print. */
function BikeTypeField({ value, error, onChange, onBlur }) {
  const { t } = useTranslation()
  const helpId = 'parameter-bikeType-help'
  const errorId = 'parameter-bikeType-error'

  return (
    <fieldset className={`${styles.field} ${styles.wideField}`}>
      <legend className={styles.label}>{t('wizard.parameters.bikeType.label')}</legend>
      <p className={styles.helpText} id={helpId}>{t('wizard.parameters.bikeType.help')}</p>
      <div className={styles.choiceCards}>
        {bikeTypeNames.map((type) => {
          const nameId = `parameter-bikeType-${type}`
          const gearingId = `${nameId}-gearing`
          return (
            <label key={type} className={styles.choiceCard}>
              <input type="radio" name="bikeType" value={type} checked={value === type}
                onChange={onChange} onBlur={onBlur} aria-invalid={Boolean(error)} aria-labelledby={nameId}
                aria-describedby={error ? `${gearingId} ${helpId} ${errorId}` : `${gearingId} ${helpId}`} />
              <span id={nameId} className={styles.choiceName}>{t(`saveAnalysis.categories.${type}`)}</span>
              <span id={gearingId} className={styles.choiceDetail}>
                {t('wizard.parameters.bikeType.gearing', {
                  chainring: bikeTypes[type].chainringTeeth, sprocket: bikeTypes[type].sprocketTeeth,
                })}
              </span>
            </label>
          )
        })}
      </div>
      {error && <p id={errorId} className={styles.error} role="alert">{t(`wizard.parameters.errors.${error}`)}</p>}
    </fieldset>
  )
}

/** A drop-down question: the wheel setup. */
function ChoiceField({ name, translationKey, optionKey, options, value, error, onChange, onBlur }) {
  const { t } = useTranslation()
  const fieldId = `parameter-${name}`
  const helpId = `${fieldId}-help`
  const errorId = `${fieldId}-error`

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={fieldId}>{t(`${translationKey}.label`)}</label>
      <p className={styles.helpText} id={helpId}>{t(`${translationKey}.help`)}</p>
      <select
        id={fieldId}
        name={name}
        className={`${styles.input} ${styles.select}`}
        value={value ?? ''}
        onChange={onChange}
        onBlur={onBlur}
        required
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${helpId} ${errorId}` : helpId}
      >
        <option value="" disabled>{t(`${translationKey}.placeholder`)}</option>
        {options.map((option) => (
          <option key={option} value={option}>{t(`${optionKey}.${option}`)}</option>
        ))}
      </select>
      {error && <p id={errorId} className={styles.error} role="alert">{t(`wizard.parameters.errors.${error}`)}</p>}
    </div>
  )
}

function ParameterStep({ parameters, points, suspensionLayout, updateWizardData }) {
  const { t } = useTranslation()
  const [touchedFields, setTouchedFields] = useState({})
  const errors = getParameterErrors(parameters)
  const calibration = getCalibration(points, parameters.eyeToEyeMm, suspensionLayout)

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
          <BikeTypeField value={parameters.bikeType} error={getVisibleError('bikeType')}
            onChange={handleChange} onBlur={handleBlur} />
          <ChoiceField name="wheelConfiguration" translationKey="wizard.parameters.wheels"
            optionKey="wizard.parameters.wheels.options" options={wheelConfigurations}
            value={parameters.wheelConfiguration} error={getVisibleError('wheelConfiguration')}
            onChange={handleChange} onBlur={handleBlur} />
          {parameterFields.map((field) => (
            <ParameterField key={field.name} field={field} value={parameters[field.name]}
              error={getVisibleError(field.name)} onChange={handleChange} onBlur={handleBlur} />
          ))}
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
