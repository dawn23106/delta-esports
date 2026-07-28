import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: { '/api': process.env.VITE_DEV_API_TARGET || 'http://localhost:8080' }
  }
})
