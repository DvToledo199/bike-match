import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useTranslation } from 'react-i18next'
import KinematicsDescriptors from './KinematicsDescriptors.jsx'
import styles from './KinematicsCharts.module.css'
import { chartAxisSize, chartMargin, getEqualScaleDomains, relativeAxlePath } from './chartGeometry.js'

function formatNumber(value, decimals = 1) {
  return Number(value).toFixed(decimals)
}

function ChartTooltip({ active, config, label, payload }) {
  const { t } = useTranslation()

  if (!active || !payload?.length) return null

  return (
    <div className={styles.tooltip}>
      <p className={styles.tooltipLabel}>
        {t(config.tooltipLabelKey, { value: formatNumber(label) })}
      </p>
      <p className={styles.tooltipValue}>
        <span className={styles.tooltipMarker} style={{ backgroundColor: config.color }} aria-hidden="true" />
        {t(config.tooltipValueKey, { value: formatNumber(payload[0].value, config.decimals) })}
      </p>
    </div>
  )
}

function ChartSummary({ config, data }) {
  const { t } = useTranslation()
  const firstPoint = data[0]
  const lastPoint = data[data.length - 1]

  return (
    <p className={styles.chartSummary}>
      {config.id === 'axle'
        ? t(config.summaryKey, {
          startX: formatNumber(firstPoint.x),
          startY: formatNumber(firstPoint.y),
          endX: formatNumber(lastPoint.x),
          endY: formatNumber(lastPoint.y),
        })
        : t(config.summaryKey, {
          start: formatNumber(firstPoint[config.yKey], config.decimals),
          end: formatNumber(lastPoint[config.yKey], config.decimals),
        })}
    </p>
  )
}

function CurveChart({ config, data, domains, square }) {
  const { t } = useTranslation()

  return (
    <section className={styles.chartCard} aria-labelledby={`${config.id}-title`}>
      <div className={styles.chartHeader}>
        <div>
          <p className={styles.chartEyebrow}>{t('wizard.charts.curveLabel')}</p>
          <h2 id={`${config.id}-title`} className={styles.chartTitle}>{t(config.titleKey)}</h2>
          <p className={styles.chartDescription}>{t(config.descriptionKey)}</p>
        </div>
        <span className={styles.chartColor} style={{ backgroundColor: config.color }} aria-hidden="true" />
      </div>

      <div className={`${styles.chartArea} ${square ? styles.squareChartArea : ''}`}>
        <ResponsiveContainer width="100%" aspect={square ? 1 : undefined} height={square ? undefined : '100%'}>
          <LineChart data={data} margin={chartMargin}>
            <CartesianGrid stroke="var(--color-border)" strokeDasharray="4 4" />
            <XAxis
              type="number"
              dataKey={config.xKey}
              domain={domains?.xDomain}
              height={chartAxisSize}
              allowDataOverflow={square}
              tickCount={5}
              tick={{ fill: 'var(--color-text-muted)', fontSize: 15, fontWeight: 600 }}
              tickFormatter={(value) => formatNumber(value)}
            />
            <YAxis
              type="number"
              dataKey={config.yKey}
              domain={domains?.yDomain}
              width={chartAxisSize}
              allowDataOverflow={square}
              tickCount={5}
              tick={{ fill: 'var(--color-text-muted)', fontSize: 15, fontWeight: 600 }}
              tickFormatter={(value) => formatNumber(value, config.decimals)}
            />
            <Tooltip content={<ChartTooltip config={config} />} cursor={{ stroke: 'var(--color-text-muted)', strokeWidth: 1 }} />
            <Line
              type="linear"
              dataKey={config.yKey}
              stroke={config.color}
              strokeWidth={4}
              dot={false}
              activeDot={{ r: 6, fill: config.color, stroke: 'var(--color-surface)', strokeWidth: 3 }}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <p className={styles.axisLabels}>{t('wizard.charts.axes', { x: t(config.xAxisKey), y: t(config.yAxisKey) })}</p>
      <ChartSummary config={config} data={data} />
    </section>
  )
}

function KinematicsCharts({ data }) {
  const { t } = useTranslation()
  const axlePoints = relativeAxlePath(data.axlePath)
  const axleDomains = getEqualScaleDomains(axlePoints)
  const charts = [
    {
      id: 'leverage',
      color: 'var(--color-chart-leverage)',
      data: data.leverageCurve,
      xKey: 'wheelTravelMm',
      yKey: 'ratio',
      decimals: 2,
      titleKey: 'wizard.charts.leverage.title',
      descriptionKey: 'wizard.charts.leverage.description',
      xAxisKey: 'wizard.charts.wheelTravelAxis',
      yAxisKey: 'wizard.charts.leverage.axis',
      tooltipLabelKey: 'wizard.charts.tooltip.wheelTravel',
      tooltipValueKey: 'wizard.charts.tooltip.leverage',
      summaryKey: 'wizard.charts.leverage.summary',
    },
    {
      id: 'kickback',
      color: 'var(--color-chart-kickback)',
      data: data.kickbackCurve,
      xKey: 'wheelTravelMm',
      yKey: 'kickbackDegrees',
      decimals: 1,
      titleKey: 'wizard.charts.kickback.title',
      descriptionKey: 'wizard.charts.kickback.description',
      xAxisKey: 'wizard.charts.wheelTravelAxis',
      yAxisKey: 'wizard.charts.kickback.axis',
      tooltipLabelKey: 'wizard.charts.tooltip.wheelTravel',
      tooltipValueKey: 'wizard.charts.tooltip.kickback',
      summaryKey: 'wizard.charts.kickback.summary',
    },
    {
      id: 'axle',
      color: 'var(--color-chart-axle)',
      data: axlePoints,
      xKey: 'x',
      yKey: 'y',
      decimals: 1,
      titleKey: 'wizard.charts.axle.title',
      descriptionKey: 'wizard.charts.axle.description',
      xAxisKey: 'wizard.charts.axle.xAxis',
      yAxisKey: 'wizard.charts.axle.yAxis',
      tooltipLabelKey: 'wizard.charts.tooltip.axleX',
      tooltipValueKey: 'wizard.charts.tooltip.axleY',
      summaryKey: 'wizard.charts.axle.summary',
    },
  ]

  return (
    <div className={styles.charts}>
      <div className={styles.introduction}>
        <h1 id="wizard-title" className={styles.title}>{t('wizard.charts.title')}</h1>
        <p className={styles.description}>{t('wizard.charts.description')}</p>
      </div>
      {charts.map((chart) => (
        <CurveChart
          key={chart.id}
          config={chart}
          data={chart.data}
          domains={chart.id === 'axle' ? axleDomains : undefined}
          square={chart.id === 'axle'}
        />
      ))}
      <KinematicsDescriptors data={data} />
    </div>
  )
}

export default KinematicsCharts
