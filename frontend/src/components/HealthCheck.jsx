import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './HealthCheck.module.css'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

function HealthCheck() {
  const { t } = useTranslation()
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    fetch(`${API_URL}/api/health`)
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        return response.json()
      })
      .then(setHealth)
      .catch(() => setError(true))
  }, [])

  if (error) {
    return (
      <p className={`${styles.card} ${styles.inline} ${styles.error}`}>
        {t('health.error')}
      </p>
    )
  }

  if (!health) {
    return (
      <p className={`${styles.card} ${styles.inline}`}>
        {t('health.loading')}
      </p>
    )
  }

  return (
    <dl className={`${styles.card} ${styles.grid} ${styles.ok}`}>
      <dt className={styles.label}>{t('health.status')}</dt>
      <dd className={`${styles.value} ${styles.okValue}`}>{health.status}</dd>
      <dt className={styles.label}>{t('health.version')}</dt>
      <dd className={`${styles.value} ${styles.okValue}`}>{health.version}</dd>
    </dl>
  )
}

export default HealthCheck
