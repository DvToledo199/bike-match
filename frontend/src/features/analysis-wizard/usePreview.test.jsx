import { act, renderHook } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import usePreview from './usePreview.js'
import { requestKinematicsPreview } from '../../services/kinematicsPreview.js'

vi.mock('../../services/kinematicsPreview.js', () => ({ requestKinematicsPreview: vi.fn() }))

it('ignores older responses and cancels when navigating back', async () => {
  let resolveOld
  let resolveNew
  requestKinematicsPreview.mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve }))
    .mockImplementationOnce(() => new Promise((resolve) => { resolveNew = resolve }))
  const { result } = renderHook(() => usePreview())
  let oldRequest, newRequest
  act(() => { oldRequest = result.current.requestPreview({}) })
  act(() => { newRequest = result.current.requestPreview({}) })
  await act(async () => { resolveNew({ id: 'new' }); await newRequest })
  await act(async () => { resolveOld({ id: 'old' }); await oldRequest })
  expect(result.current.data).toEqual({ id: 'new' })
  act(() => { result.current.cancelPreview() })
  expect(result.current.data).toBeNull()
  expect(result.current.isLoading).toBe(false)
})

it('does not publish a response after unmount', async () => {
  let resolve
  let signal
  requestKinematicsPreview.mockImplementationOnce((_data, requestSignal) => {
    signal = requestSignal
    return new Promise((done) => { resolve = done })
  })
  const { result, unmount } = renderHook(() => usePreview())
  let request
  act(() => { request = result.current.requestPreview({}) })
  unmount()
  expect(signal.aborted).toBe(true)
  await act(async () => { resolve({}); await request })
})
