import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './PhotoStep.module.css'

const acceptedMimeTypes = ['image/jpeg', 'image/png', 'image/webp']
const acceptedExtensions = ['jpg', 'jpeg', 'png', 'webp']
const maximumFileSizeBytes = 10 * 1024 * 1024
const minimumRecommendedLongestSidePixels = 750

function getFileExtension(fileName) {
  return fileName.split('.').pop()?.toLowerCase()
}

function isAcceptedImage(file) {
  if (file.type) {
    return acceptedMimeTypes.includes(file.type)
  }

  return acceptedExtensions.includes(getFileExtension(file.name))
}

function readImageDimensions(imageUrl) {
  return new Promise((resolve, reject) => {
    const image = new Image()

    image.onload = () => {
      resolve({ width: image.naturalWidth, height: image.naturalHeight })
    }
    image.onerror = reject
    image.src = imageUrl
  })
}

function PhotoStep({ photo, suspensionType, updateWizardData }) {
  const { t } = useTranslation()
  const [error, setError] = useState('')

  async function handlePhotoChange(event) {
    const [file] = event.target.files ?? []

    if (!file) return

    if (!isAcceptedImage(file)) {
      setError(t('wizard.photo.errors.invalidFormat'))
      return
    }

    if (file.size > maximumFileSizeBytes) {
      setError(t('wizard.photo.errors.fileTooLarge', { maximumSize: 10 }))
      return
    }

    const previewUrl = URL.createObjectURL(file)

    try {
      const { width, height } = await readImageDimensions(previewUrl)

      updateWizardData({
        photo: { file, previewUrl, width, height },
        points: {},
      })
      setError('')
    } catch {
      URL.revokeObjectURL(previewUrl)
      setError(t('wizard.photo.errors.unreadable'))
    }
  }

  function handlePhotoInputClick(event) {
    // Clearing immediately before the chooser opens lets the user select the same file again.
    // The newly selected file then remains visible in the native input after the change.
    event.currentTarget.value = ''
  }

  function handleSuspensionTypeChange(event) {
    updateWizardData({ suspensionType: event.target.value })
  }

  const longestSidePixels = photo ? Math.max(photo.width, photo.height) : 0
  const hasLowResolution = photo && longestSidePixels < minimumRecommendedLongestSidePixels

  return (
    <div className={styles.content}>
      <div>
        <p className={styles.eyebrow}>{t('wizard.photo.eyebrow')}</p>
        <h1 id="wizard-title" className={styles.title}>{t('wizard.photo.title')}</h1>
        <p className={styles.description}>{t('wizard.photo.description')}</p>
      </div>

      <div className={styles.section}>
        <label className={styles.label} htmlFor="bike-photo">
          {t(photo ? 'wizard.photo.changeInputLabel' : 'wizard.photo.inputLabel')}
        </label>
        <p id="bike-photo-help" className={styles.helpText}>{t('wizard.photo.inputHelp')}</p>
        <input
          id="bike-photo"
          className={styles.fileInput}
          type="file"
          accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
          onChange={handlePhotoChange}
          onClick={handlePhotoInputClick}
          aria-describedby={error ? 'bike-photo-help bike-photo-error' : 'bike-photo-help'}
        />
        {error && <p id="bike-photo-error" className={styles.error} role="alert">{error}</p>}
      </div>

      {photo && (
        <div className={styles.preview}>
          <img
            className={styles.previewImage}
            src={photo.previewUrl}
            alt={t('wizard.photo.previewAlt', { fileName: photo.file.name })}
          />
          <div className={styles.previewDetails}>
            <p className={styles.fileName}>{photo.file.name}</p>
            <p className={styles.dimensions}>{t('wizard.photo.dimensions', photo)}</p>
            {hasLowResolution && (
              <p className={styles.warning} role="status">
                {t('wizard.photo.lowResolutionWarning', {
                  minimumPixels: minimumRecommendedLongestSidePixels,
                })}
              </p>
            )}
          </div>
        </div>
      )}

      <fieldset className={styles.suspensionTypes}>
        <legend className={styles.legend}>{t('wizard.photo.suspensionLegend')}</legend>
        <p className={styles.helpText}>{t('wizard.photo.suspensionHelp')}</p>
        <label className={styles.suspensionOption}>
          <input
            type="radio"
            name="suspension-type"
            value="SINGLE_PIVOT"
            checked={suspensionType === 'SINGLE_PIVOT'}
            onChange={handleSuspensionTypeChange}
          />
          <span>{t('wizard.photo.suspensionTypes.singlePivot')}</span>
        </label>
        <label className={`${styles.suspensionOption} ${styles.unavailable}`}>
          <input type="radio" name="suspension-type" disabled />
          <span>{t('wizard.photo.suspensionTypes.fourBar')}</span>
          <span className={styles.comingSoon}>{t('wizard.photo.comingSoon')}</span>
        </label>
        <label className={`${styles.suspensionOption} ${styles.unavailable}`}>
          <input type="radio" name="suspension-type" disabled />
          <span>{t('wizard.photo.suspensionTypes.dualLink')}</span>
          <span className={styles.comingSoon}>{t('wizard.photo.comingSoon')}</span>
        </label>
      </fieldset>
    </div>
  )
}

export default PhotoStep
