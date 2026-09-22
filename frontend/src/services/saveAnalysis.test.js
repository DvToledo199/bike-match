import { beforeEach, expect, it, vi } from 'vitest'
import { ApiError, requestApi } from './apiClient.js'
import { createAnalysisSaver, createBikePayload } from './saveAnalysis.js'
import { getBikeDetail } from './myBikes.js'
import { generateBikeInterpretation } from './interpretation.js'
import { getSession } from './session.js'

vi.mock('./apiClient.js', async (original) => ({ ...await original(), requestApi: vi.fn() }))
vi.mock('./myBikes.js', () => ({ getBikeDetail: vi.fn() }))
vi.mock('./interpretation.js', () => ({ generateBikeInterpretation: vi.fn() }))
vi.mock('./session.js', () => ({ getSession: vi.fn() }))

const input = {
  username: 'rider', metadata: { brand: ' Orange ', model: 'Stage 6', modelYear: '', category: 'ENDURO', cassetteType: 'TWELVE_SPEED' },
  wizardData: { suspensionLayout: 'SINGLE_PIVOT', photo: { file: new File(['photo'], 'bike.png', { type: 'image/png' }), width: 1800, height: 1200 },
    points: { MAIN_PIVOT: { type: 'MAIN_PIVOT', x: 320.5, y: 410 } },
    parameters: { eyeToEyeMm: '230', shockStrokeMm: '65', declaredTravelMm: '160', wheelConfiguration: 'FULL_29', chainringTeeth: '32', sprocketTeeth: '50', sagPercent: '30' },
  },
}
beforeEach(() => {
  vi.resetAllMocks()
  getSession.mockReturnValue({ username: 'rider' })
  getBikeDetail.mockResolvedValue({ result: null })
  requestApi.mockImplementation(async (path) => path === '/api/bikes' ? { id: 42 } : {})
  generateBikeInterpretation.mockResolvedValue({})
})

it('maps the form to the API contract and saves metadata, original photo and natural coordinates in order', async () => {
  expect(createBikePayload(input.metadata, input.wizardData)).toMatchObject({ brand: 'Orange', modelYear: null, shockEyeToEyeMm: 230, sprocketTeeth: 50 })
  const progress = vi.fn()
  expect(await createAnalysisSaver().save(input, progress)).toEqual({ bikeId: 42, explanationReady: true })
  expect(requestApi.mock.calls.map(([path]) => path)).toEqual(['/api/bikes', '/api/bikes/42/photo', '/api/bikes/42/analysis'])
  expect(requestApi.mock.calls[1][1].body.get('photo').name).toBe('bike.png')
  expect(requestApi.mock.calls[1][1].headers).toBeUndefined()
  expect(JSON.parse(requestApi.mock.calls[2][1].body)).toEqual({ imageWidth: 1800, imageHeight: 1200, points: Object.values(input.wizardData.points) })
  expect(progress.mock.calls.flat()).toEqual(['creating', 'uploading', 'saving', 'explaining'])
})

it('retries a failed upload using the confirmed bike id instead of creating a duplicate', async () => {
  requestApi.mockResolvedValueOnce({ id: 42 }).mockRejectedValueOnce(new ApiError('server', 503))
  const saver = createAnalysisSaver()
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ status: 503 })
  expect(saver.checkpoint.bikeId).toBe(42)
  await saver.save(input, vi.fn())
  expect(requestApi.mock.calls.filter(([path]) => path === '/api/bikes')).toHaveLength(1)
  expect(requestApi.mock.calls.filter(([path]) => path.endsWith('/photo'))).toHaveLength(2)
})

it('reconciles a saved analysis after a lost finalization response', async () => {
  requestApi.mockResolvedValueOnce({ id: 42 }).mockResolvedValueOnce({}).mockRejectedValueOnce(new ApiError('network'))
  const saver = createAnalysisSaver()
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ kind: 'network' })
  getBikeDetail.mockResolvedValue({ result: { curves: {} } })
  await saver.save(input, vi.fn())
  expect(requestApi.mock.calls.filter(([path]) => path.endsWith('/analysis'))).toHaveLength(1)
  expect(requestApi.mock.calls.filter(([path]) => path.endsWith('/photo'))).toHaveLength(1)
})

it('resumes a rejected ten-point yoke analysis without recreating the bike or uploading its photo again', async () => {
  const pointTypes = [
    'MAIN_PIVOT', 'BOTTOM_BRACKET', 'HORST_PIVOT', 'ROCKER_FRAME_PIVOT',
    'ROCKER_SEATSTAY_PIVOT', 'SHOCK_FRAME', 'YOKE_ROCKER_PIVOT',
    'SHOCK_YOKE_EYE', 'REAR_AXLE', 'FRONT_AXLE',
  ]
  const points = Object.fromEntries(pointTypes.map((type, index) => [type, { type, x: 100 + index, y: 200 }]))
  const yokeInput = {
    ...input,
    wizardData: { ...input.wizardData, suspensionLayout: 'HORST_LINK_YOKE', points },
  }
  requestApi.mockResolvedValueOnce({ id: 42 }).mockResolvedValueOnce({})
    .mockRejectedValueOnce(new ApiError('invalidRequest', 400))
  const saver = createAnalysisSaver()

  await expect(saver.save(yokeInput, vi.fn())).rejects.toMatchObject({ status: 400 })
  expect(saver.checkpoint).toMatchObject({ bikeId: 42, photoUploaded: true, complete: false })
  expect(generateBikeInterpretation).not.toHaveBeenCalled()

  await expect(saver.save(yokeInput, vi.fn())).resolves.toEqual({ bikeId: 42, explanationReady: true })
  expect(requestApi.mock.calls.map(([path]) => path)).toEqual([
    '/api/bikes', '/api/bikes/42/photo', '/api/bikes/42/analysis', '/api/bikes/42/analysis',
  ])
  expect(JSON.parse(requestApi.mock.calls[0][1].body).suspensionLayout).toBe('HORST_LINK_YOKE')
  const originalRequest = JSON.parse(requestApi.mock.calls[2][1].body)
  expect(originalRequest.points).toEqual(Object.values(points))
  expect(JSON.parse(requestApi.mock.calls[3][1].body)).toEqual(originalRequest)
  expect(saver.checkpoint.complete).toBe(true)
})

it('does not retry creation automatically if the server may have created the bike', async () => {
  requestApi.mockRejectedValue(new ApiError('network'))
  const saver = createAnalysisSaver()
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ kind: 'network' })
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ kind: 'uncertainCreate' })
  expect(requestApi).toHaveBeenCalledTimes(1)
})

it('allows a known rejected creation to be corrected and never generates as a guest', async () => {
  requestApi.mockRejectedValueOnce(new ApiError('invalidRequest', 400))
  const saver = createAnalysisSaver()
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ status: 400 })
  expect(saver.checkpoint.uncertainCreate).toBe(false)
  getSession.mockReturnValue(null)
  await expect(saver.save(input, vi.fn())).rejects.toMatchObject({ status: 401 })
  expect(generateBikeInterpretation).not.toHaveBeenCalled()
})

it('keeps the saved bike available when explanation generation fails', async () => {
  generateBikeInterpretation.mockRejectedValue(new ApiError('server', 503))
  expect(await createAnalysisSaver().save(input, vi.fn())).toEqual({ bikeId: 42, explanationReady: false })
})

it('does not resume a draft using a different account', async () => {
  requestApi.mockResolvedValueOnce({ id: 42 }).mockRejectedValueOnce(new ApiError('server', 503))
  const saver = createAnalysisSaver()
  await expect(saver.save(input, vi.fn())).rejects.toBeTruthy()
  await expect(saver.save({ ...input, username: 'other' }, vi.fn())).rejects.toMatchObject({ kind: 'differentOwner' })
  expect(requestApi).toHaveBeenCalledTimes(2)
})
