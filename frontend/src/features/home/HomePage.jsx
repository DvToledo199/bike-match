import { useTranslation } from 'react-i18next'
import HealthCheck from '../../components/HealthCheck.jsx'
import styles from './HomePage.module.css'

function HomePage() {
  const { t } = useTranslation()

  return (
    <section className={styles.home}>
      <h1 className={styles.title}>{t('app.title')}</h1>
      <p className={styles.tagline}>{t('app.tagline')}</p>
      <HealthCheck />
    </section>
  )
}

export default HomePage
