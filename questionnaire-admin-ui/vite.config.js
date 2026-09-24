import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        rewrite: path => path.replace(/^\/api/, '')
      },
      '/app-api': {
        target: 'http://localhost:8083',
        rewrite: path => path.replace(/^\/app-api/, '')
      },
      '/lookup-api': {
        target: 'http://localhost:8081',
        rewrite: path => path.replace(/^\/lookup-api/, '')
      }
    }
  }
})
