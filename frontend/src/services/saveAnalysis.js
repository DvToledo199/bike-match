import { ApiError, requestApi } from './apiClient.js'
import { getBikeDetail } from './myBikes.js'
import { getSession } from './session.js'
import { generateBikeInterpretation } from './interpretation.js'

export function createBikePayload(metadata, wizardData) {
  const p = wizardData.parameters
  return {
    brand: metadata.brand.trim(), model: metadata.model.trim(),
    modelYear: metadata.modelYear === '' ? null : Number(metadata.modelYear),
    category: metadata.category, cassetteType: metadata.cassetteType,
    suspensionLayout: wizardData.suspensionLayout,
    declaredTravelMm: Number(p.declaredTravelMm), shockEyeToEyeMm: Number(p.eyeToEyeMm),
    shockStrokeMm: Number(p.shockStrokeMm), wheelConfiguration: p.wheelConfiguration,
    chainringTeeth: Number(p.chainringTeeth), sprocketTeeth: Number(p.sprocketTeeth),
    sagPercent: Number(p.sagPercent),
  }
}

// Keep confirmed steps in memory so an upload failure does not create another bike.
export function createAnalysisSaver() {
  const checkpoint = { bikeId: null, photoUploaded: false, complete: false, uncertainCreate: false, username: null }
  let running = false

  return {
    checkpoint,
    async save({ username, metadata, wizardData }, onProgress) {
      if (running) throw new ApiError('busy')
      if (checkpoint.uncertainCreate) throw new ApiError('uncertainCreate')
      if (checkpoint.username && checkpoint.username !== username) throw new ApiError('differentOwner')
      const requireOwner = () => {
        if (!username || getSession()?.username !== username) throw new ApiError('invalidCredentials', 401)
      }
      running = true
      try {
        requireOwner()
        if (!checkpoint.bikeId) {
          onProgress('creating')
          let created
          try {
            created = await requestApi('/api/bikes', {
              method: 'POST', headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(createBikePayload(metadata, wizardData)),
            })
            if (!Number.isSafeInteger(created?.id) || created.id <= 0) throw new ApiError('invalidResponse')
          } catch (error) {
            // A lost response may follow a successful creation; never blindly repeat it.
            checkpoint.uncertainCreate = !error.status || error.status >= 500
            throw error
          }
          checkpoint.bikeId = created.id
          checkpoint.username = username
        }

        requireOwner()
        // A previous finalization may have succeeded even if its response was lost.
        const existing = await getBikeDetail(checkpoint.bikeId)
        if (existing.result) checkpoint.complete = true
        if (!checkpoint.complete && !checkpoint.photoUploaded) {
          onProgress('uploading')
          const body = new FormData()
          body.append('photo', wizardData.photo.file)
          requireOwner()
          await requestApi(`/api/bikes/${checkpoint.bikeId}/photo`, { method: 'POST', body, timeoutMs: 60000 })
          checkpoint.photoUploaded = true
        }
        if (!checkpoint.complete) {
          onProgress('saving')
          requireOwner()
          await requestApi(`/api/bikes/${checkpoint.bikeId}/analysis`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              imageWidth: wizardData.photo.width, imageHeight: wizardData.photo.height,
              points: Object.values(wizardData.points).map(({ type, x, y }) => ({ type, x, y })),
            }),
          })
          checkpoint.complete = true
        }

        onProgress('explaining')
        let explanationReady = false
        try {
          requireOwner()
          await generateBikeInterpretation(checkpoint.bikeId)
          explanationReady = true
        } catch {
          // The saved bike remains available even if the explanation cannot be generated.
        }
        return { bikeId: checkpoint.bikeId, explanationReady }
      } finally {
        running = false
      }
    },
  }
}
