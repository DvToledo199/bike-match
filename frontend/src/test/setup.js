import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import '../i18n.js'

afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers() })
