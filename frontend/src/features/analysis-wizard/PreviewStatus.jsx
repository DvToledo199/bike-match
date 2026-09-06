import { lazy, Suspense } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './PreviewStatus.module.css'

const KinematicsCharts = lazy(() => import('./KinematicsCharts.jsx'))

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
      <Suspense
        fallback={(
          <section className={styles.status} aria-live="polite">
            <span className={styles.spinner} aria-hidden="true" />
            <h1 id="wizard-title" className={styles.title}>{t('wizard.preview.loadingChartsTitle')}</h1>
            <p className={styles.description}>{t('wizard.preview.loadingChartsDescription')}</p>
          </section>
        )}
      >
        <KinematicsCharts data={data} />
      </Suspense>
    )
  }

  return null
}

export default PreviewStatus
