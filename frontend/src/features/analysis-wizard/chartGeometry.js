// Equal domains only mean equal physical scales when the drawable area is square.
export const chartMargin = { top: 16, right: 16, bottom: 0, left: 0 }
export const chartAxisSize = 64

export function relativeAxlePath(points) {
  const start = points[0]
  return points.map((point) => ({ x: point.x - start.x, y: start.y - point.y }))
}

export function getEqualScaleDomains(points) {
  const xs = points.map((point) => point.x)
  const ys = points.map((point) => point.y)
  const minX = Math.min(...xs), maxX = Math.max(...xs)
  const minY = Math.min(...ys), maxY = Math.max(...ys)
  const span = Math.max(maxX - minX, maxY - minY, 1) * 1.15
  const centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2
  return {
    xDomain: [centerX - span / 2, centerX + span / 2],
    yDomain: [centerY - span / 2, centerY + span / 2],
  }
}

// Fits a value axis to the data with round ticks, so a curve fills the chart instead of hugging
// its top edge. A minimum span keeps an almost flat curve from looking dramatic.
export function getFittedAxis(values, { minimumSpan = 0.5, targetTicks = 5 } = {}) {
  const dataMin = Math.min(...values), dataMax = Math.max(...values)
  const center = (dataMin + dataMax) / 2
  const span = Math.max(dataMax - dataMin, minimumSpan)
  const rawStep = span / (targetTicks - 1)
  const magnitude = 10 ** Math.floor(Math.log10(rawStep))
  const step = [1, 2, 2.5, 5, 10].map((factor) => factor * magnitude).find((candidate) => candidate >= rawStep)
  const padding = span * 0.02
  const first = Math.floor((center - span / 2 - padding) / step)
  const last = Math.ceil((center + span / 2 + padding) / step)
  const ticks = Array.from({ length: last - first + 1 }, (_, index) => Number(((first + index) * step).toFixed(6)))
  return { yDomain: [ticks[0], ticks.at(-1)], yTicks: ticks }
}
