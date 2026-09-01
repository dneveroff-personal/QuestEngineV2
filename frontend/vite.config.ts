import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// Backend в dev-режиме поднимается отдельно (docker-compose.local.yml
// или напрямую ./gradlew bootRun) на localhost:8080. Прокси здесь
// повторяет то же разделение путей, что и nginx.conf в проде:
// /auth/* отдельно от /api/* (см. backend Routes.java) — оба ведут на
// backend, чтобы браузер с точки зрения same-origin не отличал dev от
// прода, и CORS не требовался нигде (docs/05-security/threat-model.md).
const BACKEND_URL = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': BACKEND_URL,
      '/auth': BACKEND_URL,
    },
  },
})