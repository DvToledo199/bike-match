import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

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
    return <p className="health health--error">{t('health.error')}</p>
  }

  if (!health) {
    return <p className="health">{t('health.loading')}</p>
  }

  return (
    <dl className="health health--ok">
      <dt>{t('health.status')}</dt>
      <dd>{health.status}</dd>
      <dt>{t('health.version')}</dt>
      <dd>{health.version}</dd>
    </dl>
  )
}

export default HealthCheck
