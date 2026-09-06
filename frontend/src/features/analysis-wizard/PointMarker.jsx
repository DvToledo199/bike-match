import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { hasAllPoints, pointDefinitions } from './pointDefinitions.js'
import styles from './PointMarker.module.css'

const zoomLevels = [1, 1.5, 2, 3, 4, 6, 8]
const regularNudgePixels = 0.5
const fastNudgePixels = 2

function clamp(value, minimum, maximum) {
  return Math.min(Math.max(value, minimum), maximum)
}

function getPhotoCenter(photo) {
  return { x: photo.width / 2, y: photo.height / 2 }
}

function getViewBox(photo, zoom, center) {
  const width = photo.width / zoom
  const height = photo.height / zoom

  return {
    width,
    height,
    x: clamp(center.x - width / 2, 0, photo.width - width),
    y: clamp(center.y - height / 2, 0, photo.height - height),
  }
}

function getConstrainedCenter(photo, zoom, center) {
  const width = photo.width / zoom
  const height = photo.height / zoom

  return {
    x: clamp(center.x, width / 2, photo.width - width / 2),
    y: clamp(center.y, height / 2, photo.height - height / 2),
  }
}

function PointMarker({ photo, points, updateWizardData }) {
  const { t } = useTranslation()
  const [selectedPointType, setSelectedPointType] = useState(pointDefinitions[0].type)
  const [zoom, setZoom] = useState(1)
  const [viewCenter, setViewCenter] = useState(() => getPhotoCenter(photo))
  const selectedPoint = pointDefinitions.find((point) => point.type === selectedPointType)
  const viewBox = getViewBox(photo, zoom, viewCenter)
  const markedPointCount = pointDefinitions.filter((point) => points[point.type]).length
  const allPointsMarked = hasAllPoints(points)
  const selectedPointIndex = pointDefinitions.findIndex((point) => point.type === selectedPointType)
  const previousPoint = pointDefinitions[Math.max(0, selectedPointIndex - 1)]
  const pointToUndo = points[selectedPointType] ? selectedPoint : previousPoint
  const canUndoPoint = Boolean(points[pointToUndo.type])

  function handleMarkerPointerDown(event) {
    const markerBounds = event.currentTarget.getBoundingClientRect()

    if (!markerBounds.width || !markerBounds.height) return

    event.currentTarget.focus()

    const x = clamp(
      viewBox.x + ((event.clientX - markerBounds.left) / markerBounds.width) * viewBox.width,
      0,
      photo.width,
    )
    const y = clamp(
      viewBox.y + ((event.clientY - markerBounds.top) / markerBounds.height) * viewBox.height,
      0,
      photo.height,
    )
    const pointWasAlreadyMarked = Boolean(points[selectedPointType])
    const nextPoints = {
      ...points,
      [selectedPointType]: { type: selectedPointType, x, y },
    }

    updateWizardData({ points: nextPoints })

    const nextUnmarkedPoint = pointDefinitions.find((point) => !nextPoints[point.type])
    if (!pointWasAlreadyMarked && nextUnmarkedPoint) {
      setSelectedPointType(nextUnmarkedPoint.type)
    }
  }

  function handleMarkerKeyDown(event) {
    const movements = {
      ArrowUp: { x: 0, y: -1 },
      ArrowDown: { x: 0, y: 1 },
      ArrowLeft: { x: -1, y: 0 },
      ArrowRight: { x: 1, y: 0 },
    }
    const movement = movements[event.key]
    const currentPoint = points[selectedPointType]

    if (!movement || !currentPoint) return

    event.preventDefault()
    const distance = event.shiftKey ? fastNudgePixels : regularNudgePixels
    const nextPoint = {
      ...currentPoint,
      x: clamp(currentPoint.x + movement.x * distance, 0, photo.width),
      y: clamp(currentPoint.y + movement.y * distance, 0, photo.height),
    }

    updateWizardData({
      points: {
        ...points,
        [selectedPointType]: nextPoint,
      },
    })
  }

  function changeZoom(direction) {
    const currentIndex = zoomLevels.indexOf(zoom)
    const nextZoom = zoomLevels[clamp(currentIndex + direction, 0, zoomLevels.length - 1)]

    setZoom(nextZoom)
    setViewCenter((currentCenter) => getConstrainedCenter(photo, nextZoom, currentCenter))
  }

  function panView(horizontalDirection, verticalDirection) {
    const panDistance = 0.25

    setViewCenter((currentCenter) => getConstrainedCenter(photo, zoom, {
      x: currentCenter.x + viewBox.width * panDistance * horizontalDirection,
      y: currentCenter.y + viewBox.height * panDistance * verticalDirection,
    }))
  }

  function resetView() {
    setZoom(1)
    setViewCenter(getPhotoCenter(photo))
  }

  function goToPreviousPoint() {
    if (!canUndoPoint) return

    const nextPoints = { ...points }
    delete nextPoints[pointToUndo.type]

    updateWizardData({ points: nextPoints })
    setSelectedPointType(pointToUndo.type)
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
        <p id="marker-keyboard-hint" className={styles.keyboardHint}>
          {t('wizard.marking.keyboardHint')}
        </p>
      </div>

      <div className={styles.precisionControls} aria-label={t('wizard.marking.precisionControls')}>
        <div className={styles.controlGroup}>
          <span className={styles.controlLabel}>{t('wizard.marking.correctionLabel')}</span>
          <div className={styles.controlButtons}>
            <button
              type="button"
              className={styles.resetButton}
              onClick={goToPreviousPoint}
              disabled={!canUndoPoint}
            >
              {t('wizard.marking.previousPoint')}
            </button>
          </div>
        </div>

        <div className={styles.controlGroup}>
          <span className={styles.controlLabel}>{t('wizard.marking.zoomLabel')}</span>
          <div className={styles.controlButtons}>
            <button
              type="button"
              className={styles.controlButton}
              onClick={() => changeZoom(-1)}
              disabled={zoom === zoomLevels[0]}
              aria-label={t('wizard.marking.zoomOut')}
            >
              −
            </button>
            <output className={styles.zoomValue}>{t('wizard.marking.zoomValue', { zoom: zoom * 100 })}</output>
            <button
              type="button"
              className={styles.controlButton}
              onClick={() => changeZoom(1)}
              disabled={zoom === zoomLevels[zoomLevels.length - 1]}
              aria-label={t('wizard.marking.zoomIn')}
            >
              +
            </button>
            <button type="button" className={styles.resetButton} onClick={resetView}>
              {t('wizard.marking.resetView')}
            </button>
          </div>
        </div>

        <div className={styles.controlGroup}>
          <span className={styles.controlLabel}>{t('wizard.marking.panLabel')}</span>
          <div className={styles.panButtons}>
            <button type="button" className={styles.panButton} onClick={() => panView(0, -1)} aria-label={t('wizard.marking.panUp')}>↑</button>
            <button type="button" className={styles.panButton} onClick={() => panView(-1, 0)} aria-label={t('wizard.marking.panLeft')}>←</button>
            <button type="button" className={styles.panButton} onClick={() => panView(1, 0)} aria-label={t('wizard.marking.panRight')}>→</button>
            <button type="button" className={styles.panButton} onClick={() => panView(0, 1)} aria-label={t('wizard.marking.panDown')}>↓</button>
          </div>
        </div>
      </div>

      <div className={styles.markerFrame}>
        <svg
          className={styles.marker}
          viewBox={`${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`}
          style={{ aspectRatio: `${photo.width} / ${photo.height}` }}
          aria-label={t('wizard.marking.canvasLabel')}
          aria-describedby="marker-instructions marker-keyboard-hint"
          tabIndex="0"
          onPointerDown={handleMarkerPointerDown}
          onKeyDown={handleMarkerKeyDown}
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
              <g
                key={point.type}
                className={styles.markerPoint}
                data-selected={isSelected}
              >
                <line x1={markedPoint.x - 10} y1={markedPoint.y - 10} x2={markedPoint.x + 10} y2={markedPoint.y + 10} />
                <line x1={markedPoint.x - 10} y1={markedPoint.y + 10} x2={markedPoint.x + 10} y2={markedPoint.y - 10} />
                <circle cx={markedPoint.x} cy={markedPoint.y} r="3" />
                <text x={markedPoint.x + 13} y={markedPoint.y - 13}>{index + 1}</text>
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
