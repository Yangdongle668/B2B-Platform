import { readdir, readFile } from 'node:fs/promises'
const errors = []
async function walk(path) { for (const item of await readdir(path,{ withFileTypes:true })) { if (['node_modules','.nuxt','.output','dist'].includes(item.name)) continue; const name = `${path}/${item.name}`; if (item.isDirectory()) await walk(name); else if (/\.(vue|ts|css)$/.test(name) && !name.endsWith('schema.d.ts')) { const source = await readFile(name,'utf8'); if (source.includes('v-html')) errors.push(`${name}: v-html forbidden`); if (name.endsWith('.css') && !name.includes('design-tokens') && /#[0-9a-f]{3,8}\b|\brgba?\(/i.test(source)) errors.push(`${name}: raw color outside tokens`); if (name.includes('/website/') && !name.includes('/texts/') && /[\u3400-\u9fff]/u.test(source)) errors.push(`${name}: text outside dictionary`); } } }
await walk('frontend')
if (errors.length) { console.error(errors.join('\n')); process.exit(1) }
console.log('Frontend text/token/safe-rendering checks passed')
