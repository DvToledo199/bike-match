import { useTranslation } from 'react-i18next'
import styles from './PreviewStatus.module.css'

function PreviewStatus({ data, error, isLoading, onRetry }) {
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <section className={styles.status} aria-live="polite">
        <span className={styles.spinner} aria-hidden="true" />
        <h1 id="wizard-title" className={styles.title}>{t('wizard.preview.loadingTitle')}</h1>
        <p className={styles.description}>{t('wizard.preview.loadingDescription')}</p>
      </section>
    )
  }

  if (error) {
    return (
      <section className={`${styles.status} ${styles.errorStatus}`} role="alert">
        <h1 id="wizard-title" className={styles.title}>{t('wizard.preview.errorTitle')}</h1>
        <p className={styles.description}>{t(`wizard.preview.errors.${error.kind}`)}</p>
        <button type="button" className={styles.retryButton} onClick={onRetry}>
          {t('wizard.preview.retry')}
        </button>
      </section>
    )
  }

  if (data) {
    return (
      <section className={`${styles.status} ${styles.successStatus}`} aria-live="polite">
        <h1 id="wizard-title" className={styles.title}>{t('wizard.preview.readyTitle')}</h1>
        <p className={styles.description}>{t('wizard.preview.readyDescription')}</p>
      </section>
    )
  }

  return null
}

export default PreviewStatus
