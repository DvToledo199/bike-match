import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import KinematicsCharts from '../analysis-wizard/KinematicsCharts.jsx'
import InterpretationSummary from './InterpretationSummary.jsx'
import { generateBikeInterpretation, getBikeInterpretation } from '../../services/interpretation.js'
import { getBikeDetail, toKinematicsData } from '../../services/myBikes.js'
import { getSession } from '../../services/session.js'
import styles from './BikeDetailPage.module.css'

// The API sends the layout as a code; the wizard already names each one in both languages.
const layoutKeys = { SINGLE_PIVOT: 'singlePivot', HORST_LINK: 'horstLink', HORST_LINK_YOKE: 'horstLinkYoke' }

function BikeDetailPage({ bikeId, onBack, backLabelKey = 'bikeDetail.back' }) {
  const { t, i18n } = useTranslation()
  const language = i18n.resolvedLanguage
  const [bike, setBike] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [interpretation, setInterpretation] = useState(null)
  const [interpretationError, setInterpretationError] = useState(null)
  const [interpretationLoading, setInterpretationLoading] = useState(false)
  const [interpretationGenerating, setInterpretationGenerating] = useState(false)

  useEffect(() => {
    let active = true
    setBike(null)
    setError(null)

    getBikeDetail(bikeId)
      .then((result) => {
        if (active) setBike(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError)
      })

    return () => { active = false }
  }, [bikeId, reloadKey])

  useEffect(() => {
    if (!bike) return undefined
    let active = true
    setInterpretation(null)
    setInterpretationError(null)
    setInterpretationLoading(true)

    getBikeInterpretation(bikeId, language)
      .then((result) => {
        if (active) setInterpretation(result)
      })
      .catch((requestError) => {
        if (active) setInterpretationError(requestError)
      })
      .finally(() => {
        if (active) setInterpretationLoading(false)
      })

    return () => { active = false }
  }, [bike, bikeId, language])

  async function handleGenerateInterpretation() {
    setInterpretationGenerating(true)
    setInterpretationError(null)
    try {
      setInterpretation(await generateBikeInterpretation(bikeId, language))
    } catch (requestError) {
      setInterpretationError(requestError)
    } finally {
      setInterpretationGenerating(false)
    }
  }

  if (error) {
    const message = error.status === 404
      ? t('bikeDetail.errors.notFound')
      : error.status === 401
        ? t('bikeDetail.errors.sessionExpired')
        : t('bikeDetail.errors.unavailable')

    return (
      <section className={styles.page} aria-labelledby="bike-detail-title">
        <button type="button" className={styles.backButton} onClick={onBack}>
          {t(backLabelKey)}
        </button>
        <div className={styles.message} role="alert">
          <h1 id="bike-detail-title">{t('bikeDetail.errors.title')}</h1>
          <p>{message}</p>
          <button type="button" className={styles.secondaryButton} onClick={() => setReloadKey((key) => key + 1)}>
            {t('bikeDetail.retry')}
          </button>
        </div>
      </section>
    )
  }

  if (!bike) {
    return (
      <section className={styles.page} aria-labelledby="bike-detail-title" aria-busy="true">
        <button type="button" className={styles.backButton} onClick={onBack}>
          {t(backLabelKey)}
        </button>
        <p className={styles.muted}>{t('bikeDetail.loading')}</p>
      </section>
    )
  }

  const title = [bike.brand, bike.model].filter(Boolean).join(' ') || t('myBikes.unnamed')
  const statusKey = bike.status?.toLowerCase() ?? 'unknown'
  const kinematicsData = toKinematicsData(bike)
  const session = getSession()
  const canGenerateInterpretation = Boolean(session?.username && session.username === bike.ownerUsername)

  return (
    <section className={styles.page} aria-labelledby="bike-detail-title">
      <button type="button" className={styles.backButton} onClick={onBack}>
        {t(backLabelKey)}
      </button>
      <header className={styles.heading}>
        <p className={styles.eyebrow}>{t('bikeDetail.eyebrow')}</p>
        <div className={styles.titleRow}>
          <h1 id="bike-detail-title">{title}</h1>
          <span className={`${styles.status} ${styles[`status${statusKey}`]}`}>
            {t(`myBikes.status.${statusKey}`)}
          </span>
        </div>
        <p className={styles.owner}>{t('bikeDetail.owner', { username: bike.ownerUsername })}</p>
      </header>

      <div className={styles.photoFrame}>
        {bike.photoUrl ? (
          <img src={bike.photoUrl} alt={t('myBikes.photoAlt', { name: title })} />
        ) : (
          <div className={styles.photoPlaceholder}>{t('myBikes.noPhoto')}</div>
        )}
      </div>

      <section className={styles.card} aria-labelledby="bike-specifications-title">
        <h2 id="bike-specifications-title">{t('bikeDetail.specifications')}</h2>
        <dl className={styles.specifications}>
          <div><dt>{t('myBikes.fields.year')}</dt><dd>{bike.modelYear ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.category')}</dt><dd>{bike.category ? t(`saveAnalysis.categories.${bike.category}`) : t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('bikeDetail.fields.layout')}</dt><dd>{t(`wizard.photo.suspensionLayouts.${layoutKeys[bike.suspensionLayout] ?? 'singlePivot'}`)}</dd></div>
          <div><dt>{t('bikeDetail.fields.travel')}</dt><dd>{bike.declaredTravelMm} mm</dd></div>
          <div><dt>{t('bikeDetail.fields.wheelConfiguration')}</dt><dd>{bike.wheelConfiguration ? t(`wizard.parameters.wheels.options.${bike.wheelConfiguration}`) : t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('bikeDetail.fields.sag')}</dt><dd>{bike.sagPercent}%</dd></div>
        </dl>
      </section>

      <InterpretationSummary
        interpretation={interpretation}
        error={interpretationError}
        loading={interpretationLoading}
        generating={interpretationGenerating}
        canGenerate={canGenerateInterpretation}
        onGenerate={handleGenerateInterpretation}
        onRetry={() => setReloadKey((key) => key + 1)}
      />

      {kinematicsData ? (
        <KinematicsCharts data={kinematicsData} />
      ) : (
        <div className={styles.message}>
          <p>{t('bikeDetail.noResult')}</p>
        </div>
      )}
    </section>
  )
}

export default BikeDetailPage
