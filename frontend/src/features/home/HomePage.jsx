import { useTranslation } from 'react-i18next'
import PublicBikeGallery from '../catalog/PublicBikeGallery.jsx'
import styles from './HomePage.module.css'

export default function HomePage({ onOpenBikeDetail }) {
  const { t } = useTranslation()
  return (
    <section className={styles.page} aria-labelledby="home-title">
      <header className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>{t('home.eyebrow')}</p>
          <h1 id="home-title">{t('home.title')}</h1>
          <p className={styles.description}>{t('home.description')}</p>
        </div>
        <div className={styles.action}>
          <a className={styles.primary} href="#/analyze">
            {t('app.analyze')} <span aria-hidden="true">↗</span>
          </a>
          <p>{t('home.guest')}</p>
        </div>
      </header>
      <PublicBikeGallery onOpenBikeDetail={onOpenBikeDetail} />
    </section>
  )
}
