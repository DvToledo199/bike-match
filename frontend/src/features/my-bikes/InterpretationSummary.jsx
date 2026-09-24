import { useTranslation } from 'react-i18next'
import styles from './InterpretationSummary.module.css'

const evidenceLabels = {
  usefulProgressionPercent: 'usefulProgression',
  totalProgressionPercent: 'totalProgression',
  maxRearwardMm: 'maxRearward',
  maxKickbackDegrees: 'maxKickback',
  antiSquatAtSagPercent: 'antiSquatAtSag',
  antiRiseAtSagPercent: 'antiRiseAtSag',
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
        <h2 id="interpretation-title">{t('bikeDetail.interpretation.title')}</h2>
        <p className={styles.muted}>
          {t(generating ? 'bikeDetail.interpretation.generating' : 'bikeDetail.interpretation.loading')}
        </p>
      </section>
    )
  }

  if (!interpretation) {
    const isMissing = error?.status === 404
    return (
      <section className={styles.card} aria-labelledby="interpretation-title">
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
        {canGenerate && <p className={styles.hint}>{t('bikeDetail.interpretation.generateHint')}</p>}
      </section>
    )
  }

  return (
    <section className={styles.card} aria-labelledby="interpretation-title">
      {/* The AI did not answer: say so, offer to try again, and show the rules text as a stand-in. */}
      {interpretation.fallback && canGenerate && (
        <div className={styles.notice} role="status">
          <p>{t('bikeDetail.interpretation.fallbackNotice')}</p>
          {error && (
            <p>{t(error.status === 429
              ? 'bikeDetail.interpretation.rateLimited'
              : 'bikeDetail.interpretation.unavailable')}</p>
          )}
          <button type="button" className={styles.actionButton} onClick={onGenerate}>
            {t('bikeDetail.interpretation.retryAi')}
          </button>
          <p className={styles.hint}>{t('bikeDetail.interpretation.generateHint')}</p>
        </div>
      )}
      <div className={styles.heading}>
        <p className={styles.eyebrow}>
          {t(interpretation.source === 'AI'
            ? 'bikeDetail.interpretation.aiSource'
            : 'bikeDetail.interpretation.rulesSource')}
        </p>
        <h2 id="interpretation-title">
          {t(interpretation.fallback
            ? 'bikeDetail.interpretation.fallbackTitle'
            : 'bikeDetail.interpretation.title')}
        </h2>
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
      <p className={styles.limit}>
        <strong>{t('bikeDetail.interpretation.limitLabel')}</strong> {t('bikeDetail.interpretation.limit')}
      </p>
    </section>
  )
}

export default InterpretationSummary
