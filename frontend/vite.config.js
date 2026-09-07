import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
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
