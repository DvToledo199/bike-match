import { useTranslation } from 'react-i18next'
import styles from './InterpretationSummary.module.css'

const evidenceLabels = {
  usefulProgressionPercent: 'usefulProgression',
  maxRearwardMm: 'maxRearward',
  maxKickbackDegrees: 'maxKickback',
  calculatedTravelMm: 'calculatedTravel',
  leverageRatioAtSag: 'leverageAtSag',
}

function formatValue(value) {
  return Number(value).toFixed(1).replace(/\.0$/, '')
}

function InterpretationSummary({
  interpretation,
  error,
  loading,
  generating,
  canGenerate,
  onGenerate,
  onRetry,
}) {
  const { t } = useTranslation()

  if (loading || generating) {
    return (
      <section className={styles.card} aria-labelledby="interpretation-title" aria-busy="true">
        <p className={styles.eyebrow}>{t('bikeDetail.interpretation.eyebrow')}</p>
        <h2 id="interpretation-title">{t('bikeDetail.interpretation.title')}</h2>
        <p className={styles.muted}>{t('bikeDetail.interpretation.loading')}</p>
      </section>
    )
  }

  if (!interpretation) {
    const isMissing = error?.status === 404
    return (
      <section className={styles.card} aria-labelledby="interpretation-title">
        <p className={styles.eyebrow}>{t('bikeDetail.interpretation.eyebrow')}</p>
        <h2 id="interpretation-title">{t('bikeDetail.interpretation.title')}</h2>
        <p className={styles.muted}>
          {isMissing
            ? t(canGenerate ? 'bikeDetail.interpretation.ownerMissing' : 'bikeDetail.interpretation.publicMissing')
            : t(error?.status === 429
              ? 'bikeDetail.interpretation.rateLimited'
              : 'bikeDetail.interpretation.unavailable')}
        </p>
        {(canGenerate || (!isMissing && error)) && (
          <button type="button" className={styles.actionButton} onClick={canGenerate ? onGenerate : onRetry}>
            {t(canGenerate ? 'bikeDetail.interpretation.generate' : 'bikeDetail.interpretation.retry')}
          </button>
        )}
      </section>
    )
  }

  return (
    <section className={styles.card} aria-labelledby="interpretation-title">
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>{t('bikeDetail.interpretation.eyebrow')}</p>
          <h2 id="interpretation-title">{t('bikeDetail.interpretation.title')}</h2>
        </div>
        <span className={styles.sourceBadge}>
          {t(interpretation.source === 'AI'
            ? 'bikeDetail.interpretation.aiSource'
            : 'bikeDetail.interpretation.rulesSource')}
        </span>
      </div>
      <p className={styles.summary}>{interpretation.summary}</p>
      <dl className={styles.evidence}>
        {interpretation.evidence.map((item) => (
          <div key={item.key}>
            <dt>{t(`bikeDetail.interpretation.evidence.${evidenceLabels[item.key] ?? 'unknown'}`)}</dt>
            <dd>{formatValue(item.value)} {item.unit}</dd>
          </div>
        ))}
      </dl>
      <p className={styles.limit}>{t('bikeDetail.interpretation.limit')}</p>
    </section>
  )
}

export default InterpretationSummary
