import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { pointDefinitions } from './pointDefinitions.js'
import styles from './PointMarker.module.css'

function PointMarker({ photo, points, updateWizardData }) {
  const { t } = useTranslation()
  const [selectedPointType, setSelectedPointType] = useState(pointDefinitions[0].type)
  const selectedPoint = pointDefinitions.find((point) => point.type === selectedPointType)
  const markedPointCount = pointDefinitions.filter((point) => points[point.type]).length
  const allPointsMarked = markedPointCount === pointDefinitions.length

  function handleMarkerPointerDown(event) {
    const markerBounds = event.currentTarget.getBoundingClientRect()

    if (!markerBounds.width || !markerBounds.height) return

    const x = ((event.clientX - markerBounds.left) / markerBounds.width) * photo.width
    const y = ((event.clientY - markerBounds.top) / markerBounds.height) * photo.height
    const nextPoints = {
      ...points,
      [selectedPointType]: { type: selectedPointType, x, y },
    }

    updateWizardData({ points: nextPoints })

    const nextUnmarkedPoint = pointDefinitions.find((point) => !nextPoints[point.type])
    if (nextUnmarkedPoint) {
      setSelectedPointType(nextUnmarkedPoint.type)
    }
  }

  return (
    <div className={styles.content}>
      <div>
        <p className={styles.eyebrow}>{t('wizard.marking.eyebrow')}</p>
        <h1 id="wizard-title" className={styles.title}>{t('wizard.marking.title')}</h1>
        <p className={styles.description}>{t('wizard.marking.description')}</p>
      </div>

      <div className={styles.guidance}>
        <p className={styles.progress}>
          {t('wizard.marking.progress', {
            marked: markedPointCount,
            total: pointDefinitions.length,
          })}
        </p>
        <h2 className={styles.pointName}>
          {t('wizard.marking.nowMarking', {
            point: t(`${selectedPoint.translationKey}.label`),
          })}
        </h2>
        <p className={styles.pointDescription}>{t(`${selectedPoint.translationKey}.description`)}</p>
        <p id="marker-instructions" className={styles.instructions}>
          {allPointsMarked ? t('wizard.marking.allPointsMarked') : t('wizard.marking.instructions')}
        </p>
      </div>

      <div className={styles.markerFrame}>
        <svg
          className={styles.marker}
          viewBox={`0 0 ${photo.width} ${photo.height}`}
          style={{ aspectRatio: `${photo.width} / ${photo.height}` }}
          aria-label={t('wizard.marking.canvasLabel')}
          aria-describedby="marker-instructions"
          onPointerDown={handleMarkerPointerDown}
        >
          <image
            href={photo.previewUrl}
            width={photo.width}
            height={photo.height}
            preserveAspectRatio="none"
          />
          {pointDefinitions.map((point, index) => {
            const markedPoint = points[point.type]
            if (!markedPoint) return null

            const isSelected = point.type === selectedPointType
            return (
              <g key={point.type} className={styles.markerPoint} data-selected={isSelected}>
                <circle cx={markedPoint.x} cy={markedPoint.y} r="11" />
                <text x={markedPoint.x} y={markedPoint.y} dy="0.35em">
                  {index + 1}
                </text>
              </g>
            )
          })}
        </svg>
      </div>

      <ol className={styles.pointList} aria-label={t('wizard.marking.pointListLabel')}>
        {pointDefinitions.map((point, index) => {
          const isMarked = Boolean(points[point.type])
          const isSelected = point.type === selectedPointType

          return (
            <li key={point.type}>
              <button
                type="button"
                className={styles.pointButton}
                data-selected={isSelected}
                onClick={() => setSelectedPointType(point.type)}
                aria-pressed={isSelected}
              >
                <span className={styles.pointNumber}>{index + 1}</span>
                <span className={styles.pointLabel}>{t(`${point.translationKey}.label`)}</span>
                <span className={styles.pointStatus} data-marked={isMarked}>
                  {t(isMarked ? 'wizard.marking.marked' : 'wizard.marking.toMark')}
                </span>
              </button>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

export default PointMarker
