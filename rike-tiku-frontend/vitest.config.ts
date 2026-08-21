import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // Several specs intentionally exercise the singleton router and Pinia store.
  // Keep their execution deterministic under the standard `npm test -- --run` command.
  test: { environment: 'jsdom', include: ['src/**/*.spec.ts'], fileParallelism: false },
})
