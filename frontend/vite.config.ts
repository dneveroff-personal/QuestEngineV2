import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// Backend в dev-режиме поднимается отдельно (docker-compose.local.yml
// или напрямую ./gradlew bootRun) на localhost:8080. Прокси здесь
// повторяет то же разделение путей, что и nginx.conf в проде: все API
// маршруты, включая auth, живут под /api (Routes.API — auth-контроллеры
// используют Routes.API как class-level @RequestMapping, Routes.AUTH
// лишь конкатенируется Spring-ом, а не переопределяет префикс; реальный
// путь — /api/auth/login, подтверждено по *ControllerIT в тестах backend).
// С точки зрения браузера запросы идут на localhost:5173, CORS не нужен
// ни в dev, ни в проде (docs/05-security/threat-model.md).
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
    },
  },
})