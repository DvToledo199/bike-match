export const minimumZoom = 1
export const maximumZoom = 12
export const clamp = (value, minimum, maximum) => Math.min(Math.max(value, minimum), maximum)

export function getViewBox(photo, zoom, center, aspect = photo.width / photo.height) {
  const width = Math.max(photo.width, photo.height * aspect) / zoom
  const height = width / aspect
  return {
    width, height,
    x: width >= photo.width ? (photo.width - width) / 2 : clamp(center.x - width / 2, 0, photo.width - width),
    y: height >= photo.height ? (photo.height - height) / 2 : clamp(center.y - height / 2, 0, photo.height - height),
  }
}

export function constrainView(photo, view, aspect) {
  const box = getViewBox(photo, view.zoom, view.center, aspect)
  return { zoom: view.zoom, center: { x: box.x + box.width / 2, y: box.y + box.height / 2 } }
}

export function zoomAt(photo, view, nextZoom, anchor, aspect) {
  const zoom = clamp(nextZoom, minimumZoom, maximumZoom)
  const oldBox = getViewBox(photo, view.zoom, view.center, aspect)
  const newBox = getViewBox(photo, zoom, view.center, aspect)
  const focus = anchor ?? { x: 0.5, y: 0.5 }
  return constrainView(photo, { zoom, center: {
    x: oldBox.x + focus.x * oldBox.width + (0.5 - focus.x) * newBox.width,
    y: oldBox.y + focus.y * oldBox.height + (0.5 - focus.y) * newBox.height,
  } }, aspect)
}

export function imagePoint(box, bounds, clientX, clientY) {
  return { x: box.x + (clientX - bounds.left) / bounds.width * box.width,
    y: box.y + (clientY - bounds.top) / bounds.height * box.height }
}
