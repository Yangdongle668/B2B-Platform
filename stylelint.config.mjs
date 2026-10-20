export default {
  ignoreFiles: ['**/node_modules/**','**/.nuxt/**','**/.output/**','**/dist/**'],
  overrides: [{ files: ['**/*.vue'], customSyntax: 'postcss-html' }],
  rules: { 'color-no-invalid-hex': true, 'block-no-empty': true, 'declaration-block-no-duplicate-properties': true, 'selector-pseudo-class-no-unknown': [true, { ignorePseudoClasses: ['deep','global','slotted'] }], 'property-no-unknown': true },
}
