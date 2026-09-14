import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { listPublicBikes } from '../../services/myBikes.js'
import styles from './CatalogPage.module.css'

const categories = [
  { value: '', labelKey: 'catalog.filters.all' },
  { value: 'ENDURO', labelKey: 'catalog.filters.enduro' },
  { value: 'E_ENDURO', labelKey: 'catalog.filters.eEnduro' },
  { value: 'DOWNHILL', labelKey: 'catalog.filters.downhill' },
]

function CatalogPage({ onOpenBikeDetail }) {
  const { t } = useTranslation()
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [catalog, setCatalog] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setCatalog(null)
    setError(null)

    listPublicBikes({ category, page })
      .then((result) => {
        if (active) setCatalog(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError)
      })

    return () => { active = false }
  }, [category, page, reloadKey])

  function changeCategory(event) {
    setCategory(event.target.value)
    setPage(0)
  }

  return (
    <section className={styles.page} aria-labelledby="catalog-title">
      <header className={styles.heading}>
        <p className={styles.eyebrow}>{t('catalog.eyebrow')}</p>
        <h1 id="catalog-title">{t('catalog.title')}</h1>
        <p className={styles.description}>{t('catalog.description')}</p>
      </header>

      <div className={styles.controls}>
        <label htmlFor="catalog-category">{t('catalog.filterLabel')}</label>
        <select id="catalog-category" value={category} onChange={changeCategory}>
          {categories.map((option) => (
            <option key={option.value} value={option.value}>{t(option.labelKey)}</option>
          ))}
        </select>
      </div>

      {error ? (
        <div className={styles.message} role="alert">
          <p>{t('catalog.errors.unavailable')}</p>
          <button type="button" className={styles.secondaryButton} onClick={() => setReloadKey((key) => key + 1)}>
            {t('catalog.retry')}
          </button>
        </div>
      ) : !catalog ? (
        <p className={styles.muted} aria-busy="true">{t('catalog.loading')}</p>
      ) : catalog.items.length === 0 ? (
        <div className={styles.message}>
          <p>{t('catalog.empty')}</p>
        </div>
      ) : (
        <>
          <div className={styles.grid}>
            {catalog.items.map((bike) => <BikeCard key={bike.id} bike={bike} t={t} onOpen={() => onOpenBikeDetail(bike.id)} />)}
          </div>
          <nav className={styles.pagination} aria-label={t('catalog.pagination.label')}>
            <button type="button" className={styles.secondaryButton} disabled={catalog.page === 0} onClick={() => setPage((current) => current - 1)}>
              {t('catalog.pagination.previous')}
            </button>
            <span>{t('catalog.pagination.page', { page: catalog.page + 1, total: Math.max(catalog.totalPages, 1) })}</span>
            <button type="button" className={styles.secondaryButton} disabled={!catalog.hasNext} onClick={() => setPage((current) => current + 1)}>
              {t('catalog.pagination.next')}
            </button>
          </nav>
        </>
      )}
    </section>
  )
}

function BikeCard({ bike, t, onOpen }) {
  const title = [bike.brand, bike.model].filter(Boolean).join(' ')
  const categoryKey = bike.category === 'E_ENDURO' ? 'eEnduro' : bike.category?.toLowerCase()

  return (
    <article className={styles.card}>
      {bike.photoUrl ? (
        <img className={styles.photo} src={bike.photoUrl} alt={t('catalog.photoAlt', { name: title })} />
      ) : (
        <div className={styles.photoPlaceholder}>{t('catalog.noPhoto')}</div>
      )}
      <div className={styles.cardBody}>
        <h2>{title || t('catalog.unnamed')}</h2>
        <dl className={styles.details}>
          <div><dt>{t('myBikes.fields.year')}</dt><dd>{bike.modelYear ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.category')}</dt><dd>{categoryKey ? t(`catalog.filters.${categoryKey}`) : t('myBikes.notAvailable')}</dd></div>
        </dl>
        <button type="button" className={styles.primaryButton} onClick={onOpen}>
          {t('catalog.openDetail')}
        </button>
      </div>
    </article>
  )
}

export default CatalogPage
