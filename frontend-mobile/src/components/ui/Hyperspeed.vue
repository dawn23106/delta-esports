<template>
  <canvas ref="canvasRef" class="hyperspeed-canvas" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  color?: string
  speed?: number
}>(), {
  color: '#6366f1',
  speed: 1,
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
let animId = 0
let stars: { x: number; y: number; z: number; speed: number }[] = []

function init() {
  if (!canvasRef.value) return
  const c = canvasRef.value
  c.width = window.innerWidth
  c.height = window.innerHeight
  const cx = c.width / 2
  const cy = c.height / 2
  stars = Array.from({ length: 200 }, () => ({
    x: (Math.random() - 0.5) * c.width * 1.5,
    y: (Math.random() - 0.5) * c.height * 1.5,
    z: Math.random() * c.width,
    speed: (1 + Math.random() * 3) * props.speed,
  }))
}

function draw() {
  if (!canvasRef.value) return
  const c = canvasRef.value
  const ctx = c.getContext('2d')!
  const cx = c.width / 2
  const cy = c.height / 2

  ctx.fillStyle = 'rgba(0,0,0,.15)'
  ctx.fillRect(0, 0, c.width, c.height)

  for (const s of stars) {
    s.z -= s.speed
    if (s.z <= 1) { s.z = c.width; s.x = (Math.random() - 0.5) * c.width * 1.5; s.y = (Math.random() - 0.5) * c.height * 1.5 }
    const px = s.x / (s.z / c.width) + cx
    const py = s.y / (s.z / c.width) + cy
    const r = Math.max(0.5, (1 - s.z / c.width) * 3)
    const alpha = Math.max(0, 1 - s.z / c.width)
    ctx.fillStyle = props.color
    ctx.globalAlpha = alpha
    ctx.beginPath(); ctx.arc(px, py, r, 0, Math.PI * 2); ctx.fill()
  }
  ctx.globalAlpha = 1
  animId = requestAnimationFrame(draw)
}

function onResize() { init() }

onMounted(() => {
  init(); draw()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.hyperspeed-canvas { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
</style>
