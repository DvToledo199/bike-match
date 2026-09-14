import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { listMyBikes } from '../../services/myBikes.js'
import styles from './MyBikesPage.module.css'

function MyBikesPage() {
  const { t } = useTranslation()
  const [bikes, setBikes] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setBikes(null)
    setError(null)

    listMyBikes()
      .then((result) => {
        if (active) setBikes(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError)
      })

    return () => { active = false }
  }, [reloadKey])

  if (error) {
    const message = error.status === 401
      ? t('myBikes.errors.sessionExpired')
      : t('myBikes.errors.unavailable')

    return (
      <section className={styles.page} aria-labelledby="my-bikes-title">
        <PageHeading t={t} />
        <div className={`${styles.message} ${styles.error}`} role="alert">
          <p>{message}</p>
          <button type="button" className={styles.secondaryButton} onClick={() => setReloadKey((key) => key + 1)}>
            {t('myBikes.retry')}
          </button>
        </div>
      </section>
    )
  }

  if (!bikes) {
    return (
      <section className={styles.page} aria-labelledby="my-bikes-title" aria-busy="true">
        <PageHeading t={t} />
        <p className={styles.muted}>{t('myBikes.loading')}</p>
      </section>
    )
  }

  return (
    <section className={styles.page} aria-labelledby="my-bikes-title">
      <PageHeading t={t} />
      {bikes.length === 0 ? (
        <div className={styles.message}>
          <p>{t('myBikes.empty')}</p>
        </div>
      ) : (
        <div className={styles.grid}>
          {bikes.map((bike) => <BikeCard key={bike.id} bike={bike} t={t} />)}
        </div>
      )}
    </section>
  )
}

function PageHeading({ t }) {
  return (
    <header className={styles.heading}>
      <p className={styles.eyebrow}>{t('myBikes.eyebrow')}</p>
      <h1 id="my-bikes-title">{t('myBikes.title')}</h1>
      <p className={styles.description}>{t('myBikes.description')}</p>
    </header>
  )
}

function BikeCard({ bike, t }) {
  const statusKey = bike.status?.toLowerCase() ?? 'unknown'
  const title = [bike.brand, bike.model].filter(Boolean).join(' ')

  return (
    <article className={styles.card}>
      {bike.photoUrl ? (
        <img className={styles.photo} src={bike.photoUrl} alt={t('myBikes.photoAlt', { name: title })} />
      ) : (
        <div className={styles.photoPlaceholder} aria-hidden="true">{t('myBikes.noPhoto')}</div>
      )}
      <div className={styles.cardBody}>
        <div className={styles.cardHeading}>
          <h2>{title || t('myBikes.unnamed')}</h2>
          <span className={`${styles.status} ${styles[`status${statusKey}`]}`}>
            {t(`myBikes.status.${statusKey}`)}
          </span>
        </div>
        <dl className={styles.details}>
          <div><dt>{t('myBikes.fields.year')}</dt><dd>{bike.modelYear ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.category')}</dt><dd>{bike.category ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.analysis')}</dt><dd>{bike.analyzed ? t('myBikes.analysisReady') : t('myBikes.analysisPending')}</dd></div>
        </dl>
      </div>
    </article>
  )
}

export default MyBikesPage
