import { useTranslation } from 'react-i18next'
import PublicBikeGallery from './PublicBikeGallery.jsx'
import styles from './CatalogPage.module.css'

export default function CatalogPage({ onOpenBikeDetail }) {
  const { t } = useTranslation()
  return (
    <section className={styles.page} aria-labelledby="catalog-title">
      <header className={styles.heading}>
        <p className={styles.eyebrow}>{t('catalog.eyebrow')}</p>
        <h1 id="catalog-title">{t('catalog.title')}</h1>
        <p className={styles.description}>{t('catalog.description')}</p>
      </header>
      <PublicBikeGallery onOpenBikeDetail={onOpenBikeDetail} />
    </section>
  )
}
