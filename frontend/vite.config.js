import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'

// 开发服务器默认 5173；/api 与登录相关路径代理到本地 Spring Boot (8080)
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag.startsWith('ion-')
        }
      }
    }),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['pwa/*.png', 'ionicons/**/*'],
      manifest: {
        name: 'LMS 智慧图书馆管理系统',
        short_name: 'LMS',
        description: '图书借阅与馆藏管理系统（Vue3 + Spring Boot）',
        lang: 'zh-CN',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'portrait',
        background_color: '#080b1c',
        theme_color: '#6366f1',
        icons: [
          { src: 'pwa/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa/icon-512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}', 'ionicons/**/*.{js,svg}'],
        navigateFallback: '/index.html',
        cleanupOutdatedCaches: true,
        clientsClaim: true
      },
      devOptions: { enabled: false }
    })
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/login': { target: 'http://localhost:8080', changeOrigin: true },
      '/register': { target: 'http://localhost:8080', changeOrigin: true },
      '/forgot-password': { target: 'http://localhost:8080', changeOrigin: true },
      '/reset-password': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
    chunkSizeWarningLimit: 1024
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.{js,ts}']
  }
})
