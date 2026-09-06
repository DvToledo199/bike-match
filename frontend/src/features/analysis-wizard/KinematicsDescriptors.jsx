import { useTranslation } from 'react-i18next'
import styles from './KinematicsDescriptors.module.css'
import { getKickbackLevel } from './chartGeometry.js'

function formatNumber(value, decimals = 1) {
  return Number(value).toFixed(decimals)
}

function Metric({ label, value }) {
  return (
    <div className={styles.metric}>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function TrendBadge({ trend }) {
  const { t } = useTranslation()

  return <span className={styles.badge}>{t(`wizard.descriptors.trends.${trend}`)}</span>
}

function KinematicsDescriptors({ data }) {
  const { t } = useTranslation()
  const { leverageDescriptors, axlePathDescriptors, travelCheck, conditions } = data
  const kickbackLevel = getKickbackLevel(data.kickbackCurve)
  const phases = [
    { labelKey: 'initial', trend: leverageDescriptors.initialTrend },
    { labelKey: 'middle', trend: leverageDescriptors.middleTrend },
    { labelKey: 'final', trend: leverageDescriptors.finalTrend },
  ]

  return (
    <section className={styles.descriptors} aria-labelledby="descriptors-title">
      <div className={styles.introduction}>
        <p className={styles.eyebrow}>{t('wizard.descriptors.eyebrow')}</p>
        <h2 id="descriptors-title">{t('wizard.descriptors.title')}</h2>
        <p>{t('wizard.descriptors.description')}</p>
      </div>

      <section className={styles.card} aria-labelledby="leverage-descriptors-title">
        <div className={styles.cardHeading}>
          <div>
            <h3 id="leverage-descriptors-title">{t('wizard.descriptors.leverage.title')}</h3>
            <p>{t('wizard.descriptors.leverage.description')}</p>
          </div>
          <span className={styles.bandBadge}>
            {t(`wizard.descriptors.progressionBands.${leverageDescriptors.progressionBand}`)}
          </span>
        </div>
        <dl className={styles.metricGrid}>
          <Metric label={t('wizard.descriptors.leverage.initial')} value={formatNumber(leverageDescriptors.lrInitial, 2)} />
          <Metric label={t('wizard.descriptors.leverage.atSag')} value={formatNumber(leverageDescriptors.lrAtSag, 2)} />
          <Metric label={t('wizard.descriptors.leverage.final')} value={formatNumber(leverageDescriptors.lrFinal, 2)} />
          <Metric label={t('wizard.descriptors.leverage.average')} value={formatNumber(leverageDescriptors.lrMean, 2)} />
          <Metric label={t('wizard.descriptors.leverage.totalProgression')} value={`${formatNumber(leverageDescriptors.totalProgressionPercent)}%`} />
          <Metric label={t('wizard.descriptors.leverage.usefulProgression')} value={`${formatNumber(leverageDescriptors.usefulProgressionPercent)}%`} />
        </dl>
      </section>

      <section className={styles.card} aria-labelledby="shape-title">
        <div className={styles.cardHeading}>
          <div>
            <h3 id="shape-title">{t('wizard.descriptors.shape.title')}</h3>
            <p>{t('wizard.descriptors.shape.description')}</p>
          </div>
        </div>
        <ul className={styles.phaseList}>
          {phases.map((phase) => (
            <li key={phase.labelKey} className={styles.phase}>
              <span>{t(`wizard.descriptors.shape.phases.${phase.labelKey}`)}</span>
              <TrendBadge trend={phase.trend} />
            </li>
          ))}
        </ul>
      </section>

      <section className={styles.card} aria-labelledby="axle-descriptors-title">
        <div className={styles.cardHeading}>
          <div>
            <h3 id="axle-descriptors-title">{t('wizard.descriptors.axle.title')}</h3>
            <p>{t('wizard.descriptors.axle.description')}</p>
          </div>
        </div>
        <dl className={styles.metricGrid}>
          <Metric label={t('wizard.descriptors.axle.maxRearward')} value={`${formatNumber(axlePathDescriptors.maxRearwardMm)} mm`} />
          <Metric label={t('wizard.descriptors.axle.atTravel')} value={`${formatNumber(axlePathDescriptors.atTravelPercent)}%`} />
        </dl>
      </section>

      <section className={styles.card} aria-labelledby="kickback-descriptors-title">
        <div className={styles.cardHeading}>
          <div>
            <h3 id="kickback-descriptors-title">{t('wizard.descriptors.kickback.title')}</h3>
            <p>{t('wizard.descriptors.kickback.description')}</p>
          </div>
          <span className={styles.bandBadge}>{t(`wizard.descriptors.kickback.levels.${kickbackLevel}`)}</span>
        </div>
        <p className={styles.conditions}>
          {t('wizard.descriptors.kickback.conditions', {
            sag: formatNumber(conditions.sagPercent),
            chainring: conditions.chainringTeeth,
            sprocket: conditions.sprocketTeeth,
          })}
        </p>
      </section>

      <section
        className={`${styles.travelCheck} ${travelCheck.withinTolerance ? styles.travelCheckOk : styles.travelCheckWarning}`}
        aria-labelledby="travel-check-title"
      >
        <h3 id="travel-check-title">{t('wizard.descriptors.travel.title')}</h3>
        <p>
          {t(travelCheck.withinTolerance
            ? 'wizard.descriptors.travel.withinTolerance'
            : 'wizard.descriptors.travel.outsideTolerance', {
            calculated: formatNumber(travelCheck.calculatedTravelMm),
            declared: formatNumber(travelCheck.declaredTravelMm),
            deviation: formatNumber(travelCheck.deviationPercent),
          })}
        </p>
      </section>
    </section>
  )
}

export default KinematicsDescriptors
