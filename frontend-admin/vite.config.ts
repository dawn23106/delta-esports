import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [vue(), tailwindcss()],
    server: {
      port: 5174,
      proxy: {
        '/api': { target: 'http://localhost:8080', changeOrigin: true }
      }
    }
  }
})
