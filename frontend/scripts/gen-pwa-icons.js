// 生成 PWA 清单所需的图标（纯 Node 实现，无第三方依赖，可重复执行）
// 用法：node scripts/gen-pwa-icons.js
import { deflateSync } from 'node:zlib'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = join(root, 'public', 'pwa')
mkdirSync(outDir, { recursive: true })

const CRC_TABLE = (() => {
    const t = new Uint32Array(256)
    for (let n = 0; n < 256; n++) {
        let c = n
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
        t[n] = c >>> 0
    }
    return t
})()

function crc32(buf) {
    let c = 0xffffffff
    for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8)
    return (c ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
    const len = Buffer.alloc(4)
    len.writeUInt32BE(data.length)
    const typeBuf = Buffer.from(type, 'ascii')
    const crcBuf = Buffer.alloc(4)
    crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])))
    return Buffer.concat([len, typeBuf, data, crcBuf])
}

function makePng(size, rgba) {
    const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
    const ihdr = Buffer.alloc(13)
    ihdr.writeUInt32BE(size, 0)
    ihdr.writeUInt32BE(size, 4)
    ihdr[8] = 8 // bit depth
    ihdr[9] = 6 // color type RGBA
    const raw = Buffer.alloc((size * 4 + 1) * size)
    for (let y = 0; y < size; y++) {
        raw[y * (size * 4 + 1)] = 0 // filter none
        rgba.copy(raw, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4)
    }
    return Buffer.concat([
        sig,
        chunk('IHDR', ihdr),
        chunk('IDAT', deflateSync(raw, { level: 9 })),
        chunk('IEND', Buffer.alloc(0))
    ])
}

// 品牌渐变：indigo (#6366f1) → violet (#a855f7)，掩码图四周留白边
function gradientRgba(size, safeInset = 0) {
    const buf = Buffer.alloc(size * size * 4)
    const inset = Math.round(size * safeInset)
    const top = { r: 99, g: 102, b: 241 }
    const bottom = { r: 168, g: 85, b: 247 }
    for (let y = 0; y < size; y++) {
        const t = y / (size - 1 || 1)
        const r = Math.round(top.r + (bottom.r - top.r) * t)
        const g = Math.round(top.g + (bottom.g - top.g) * t)
        const b = Math.round(top.b + (bottom.b - top.b) * t)
        for (let x = 0; x < size; x++) {
            const o = (y * size + x) * 4
            const inside = x >= inset && x < size - inset && y >= inset && y < size - inset
            buf[o] = r
            buf[o + 1] = g
            buf[o + 2] = b
            buf[o + 3] = inside ? 255 : 0
        }
    }
    return buf
}

writeFileSync(join(outDir, 'icon-192.png'), makePng(192, gradientRgba(192)))
writeFileSync(join(outDir, 'icon-512.png'), makePng(512, gradientRgba(512)))
writeFileSync(join(outDir, 'icon-512-maskable.png'), makePng(512, gradientRgba(512, 0.12)))
console.log('PWA icons generated in', outDir)
