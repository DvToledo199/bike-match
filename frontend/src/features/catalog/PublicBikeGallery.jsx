import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { listPublicBikes } from '../../services/myBikes.js'
import styles from './PublicBikeGallery.module.css'

const categories = [
  { value: '', key: 'all' },
  { value: 'ENDURO', key: 'enduro' },
  { value: 'E_ENDURO', key: 'eEnduro' },
  { value: 'DOWNHILL', key: 'downhill' },
]

export default function PublicBikeGallery({ onOpenBikeDetail }) {
  const { t } = useTranslation()
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [catalog, setCatalog] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [waking, setWaking] = useState(false)

  useEffect(() => {
    let active = true
    setCatalog(null)
    setError(null)
    setWaking(false)
    // A catalog still loading after a few seconds means the server is waking up: say so.
    const wakingTimer = setTimeout(() => { if (active) setWaking(true) }, 5000)
    listPublicBikes({ category, page })
      .then((result) => { if (active) setCatalog(result) })
      .catch((requestError) => { if (active) setError(requestError) })
      .finally(() => clearTimeout(wakingTimer))
    return () => {
      active = false
      clearTimeout(wakingTimer)
    }
  }, [category, page, reloadKey])

  function filter(nextCategory) {
    setCategory(nextCategory)
    setPage(0)
  }

  return (
    <div className={styles.gallery}>
      <div className={styles.filters} role="group" aria-label={t('catalog.filterLabel')}>
        {categories.map((option) => (
          <button key={option.value} type="button" aria-pressed={category === option.value}
            onClick={() => filter(option.value)}>
            {t(`catalog.filters.${option.key}`)}
          </button>
        ))}
      </div>

      {error ? (
        <div className={styles.message} role="alert">
          <h2>{t('catalog.errors.title')}</h2>
          <p>{t('catalog.errors.unavailable')}</p>
          <button className={styles.secondary} type="button" onClick={() => setReloadKey((key) => key + 1)}>
            {t('catalog.retry')}
          </button>
        </div>
      ) : !catalog ? (
        <div aria-busy="true">
          <p className={styles.muted} role="status">{t(waking ? 'catalog.waking' : 'catalog.loading')}</p>
          <div className={styles.grid} aria-hidden="true">
            {[0, 1, 2].map((key) => <div key={key} className={styles.skeleton} />)}
          </div>
        </div>
      ) : catalog.items.length === 0 ? (
        <div className={styles.message} role="status">
          <h2>{t(category ? 'catalog.emptyCategoryTitle' : 'catalog.emptyTitle')}</h2>
          <p>{t(category ? 'catalog.emptyCategory' : 'catalog.empty')}</p>
          {category ? (
            <button className={styles.secondary} type="button" onClick={() => filter('')}>{t('catalog.showAll')}</button>
          ) : (
            <a className={styles.secondary} href="#/analyze">{t('catalog.addFirst')}</a>
          )}
        </div>
      ) : (
        <>
          <div className={styles.grid}>
            {catalog.items.map((bike) => (
              <PublicBikeCard key={bike.id} bike={bike} onOpenBikeDetail={onOpenBikeDetail} />
            ))}
          </div>
          {catalog.totalPages > 1 && (
            <nav className={styles.pagination} aria-label={t('catalog.pagination.label')}>
              <button type="button" className={styles.secondary} disabled={catalog.page === 0}
                onClick={() => setPage((current) => current - 1)}>{t('catalog.pagination.previous')}</button>
              <span role="status">{t('catalog.pagination.page', { page: catalog.page + 1, total: catalog.totalPages })}</span>
              <button type="button" className={styles.secondary} disabled={!catalog.hasNext}
                onClick={() => setPage((current) => current + 1)}>{t('catalog.pagination.next')}</button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

function PublicBikeCard({ bike, onOpenBikeDetail }) {
  const { t } = useTranslation()
  const [failedPhoto, setFailedPhoto] = useState(null)
  const title = [bike.brand, bike.model].filter(Boolean).join(' ') || t('catalog.unnamed')
  const category = categories.find((option) => option.value === bike.category && option.value)

  function open(event) {
    if (!onOpenBikeDetail || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    onOpenBikeDetail(bike.id)
  }

  return (
    <article className={styles.card}>
      <a className={styles.cardLink} href={`#/bikes/${bike.id}`} onClick={open}
        aria-label={t('catalog.openBike', { name: title })}>
        <div className={styles.photoFrame}>
          {bike.photoUrl && failedPhoto !== bike.photoUrl ? (
            <img src={bike.photoUrl} alt={t('catalog.photoAlt', { name: title })}
              loading="lazy" decoding="async" onError={() => setFailedPhoto(bike.photoUrl)} />
          ) : (
            <div className={styles.noPhoto}>{t('catalog.noPhoto')}</div>
          )}
        </div>
        <div className={styles.cardBody}>
          <div className={styles.metadata}>
            <span>{category ? t(`catalog.filters.${category.key}`) : t('myBikes.notAvailable')}</span>
            {bike.modelYear && <span>{bike.modelYear}</span>}
          </div>
          <h2>{title}</h2>
          <span className={styles.open}>{t('catalog.openDetail')} <span aria-hidden="true">↗</span></span>
        </div>
      </a>
    </article>
  )
}
