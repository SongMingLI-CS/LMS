/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', '"Segoe UI"', '"PingFang SC"', '"Hiragino Sans GB"', '"Microsoft YaHei"', 'sans-serif']
      },
      boxShadow: {
        soft: '0 1px 2px 0 rgb(15 23 42 / 0.04), 0 8px 24px -8px rgb(15 23 42 / 0.10)',
        glow: '0 10px 28px -8px rgb(99 102 241 / 0.5)',
        glass: '0 1px 3px 0 rgb(15 23 42 / 0.05), 0 16px 44px -14px rgb(79 70 229 / 0.22)'
      }
    }
  },
  plugins: []
}
