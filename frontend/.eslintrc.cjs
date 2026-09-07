module.exports = {
    root: true,
    env: { browser: true, es2022: true, node: true },
    extends: ['eslint:recommended', 'plugin:vue/vue3-essential', 'prettier'],
    parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
    ignorePatterns: ['node_modules/**', 'dist/**', 'public/**', 'src/main/resources/**'],
    rules: {
        'vue/multi-word-component-names': 'off',
        'no-unused-vars': 'warn',
        // 兼容历史 catch(e){} 空块（保留默认 error 语义并允许空 catch）
        'no-empty': ['error', { allowEmptyCatch: true }]
    },
    overrides: [
        {
            files: ['*.vue'],
            parser: 'vue-eslint-parser',
            parserOptions: {
                parser: '@typescript-eslint/parser',
                ecmaVersion: 'latest',
                sourceType: 'module'
            }
        },
        {
            files: ['*.ts', '*.tsx', '*.mts'],
            parser: '@typescript-eslint/parser',
            parserOptions: { ecmaVersion: 'latest', sourceType: 'module' }
        }
    ]
}
