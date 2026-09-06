import { act, fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import PhotoStep from './PhotoStep.jsx'

it('keeps the accepted photo visible when the chooser is cancelled or the step is reopened', () => {
  const update = vi.fn()
  const photo = { file: { name: 'bike.jpg' }, previewUrl: 'blob:accepted', width: 1800, height: 1200 }
  const { container, unmount } = render(<PhotoStep photo={photo} updateWizardData={update} />)
  fireEvent.change(container.querySelector('input[type=file]'), { target: { files: [] } })
  expect(screen.getByText('bike.jpg')).toBeTruthy()
  expect(update).not.toHaveBeenCalled()
  unmount()
  render(<PhotoStep photo={photo} updateWizardData={update} />)
  expect(screen.getByText('bike.jpg')).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Choose a different photo' })).toBeTruthy()
})

it('ignores stale decodes and releases their URLs, including after unmount', async () => {
  const images = []
  vi.stubGlobal('Image', class {
    constructor() { this.naturalWidth = 1800; this.naturalHeight = 1200; images.push(this) }
  })
  const revoke = vi.fn()
  vi.stubGlobal('URL', { createObjectURL: (file) => `blob:${file.name}`, revokeObjectURL: revoke })
  const update = vi.fn()
  const { container, unmount } = render(<PhotoStep updateWizardData={update} />)
  const input = container.querySelector('input[type=file]')
  for (const name of ['first.jpg', 'second.jpg']) {
    fireEvent.change(input, { target: { files: [new File(['photo'], name, { type: 'image/jpeg' })] } })
  }
  await act(async () => { images[1].onload() })
  await act(async () => { images[0].onload() })
  expect(update).toHaveBeenCalledTimes(1)
  expect(update.mock.calls[0][0].photo.file.name).toBe('second.jpg')
  expect(revoke).toHaveBeenCalledWith('blob:first.jpg')
  fireEvent.change(input, { target: { files: [new File(['photo'], 'third.jpg', { type: 'image/jpeg' })] } })
  unmount()
  await act(async () => { images[2].onload() })
  expect(revoke).toHaveBeenCalledWith('blob:third.jpg')
  expect(update).toHaveBeenCalledTimes(1)
})
