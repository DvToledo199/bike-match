import { Component } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './PreviewStatus.module.css'

// Keep the small MVP in one load: a rejected lazy import is cached by React and
// cannot be recovered by merely recalculating the bike.
import KinematicsCharts from './KinematicsCharts.jsx'

class ChartLoadBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback
    }

    return this.props.children
  }
}

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
      <ChartLoadBoundary
        fallback={(
          <section className={`${styles.status} ${styles.errorStatus}`} role="alert">
            <h1 id="wizard-title" className={styles.title}>{t('wizard.preview.chartErrorTitle')}</h1>
            <p className={styles.description}>{t('wizard.preview.chartErrorDescription')}</p>
            <button type="button" className={styles.retryButton} onClick={onRetry}>
              {t('wizard.preview.retry')}
            </button>
          </section>
        )}
      >
        <KinematicsCharts data={data} />
      </ChartLoadBoundary>
    )
  }

  return null
}

export default PreviewStatus
