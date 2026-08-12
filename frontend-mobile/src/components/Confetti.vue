<template>
  <canvas ref="canvasRef" class="confetti-canvas" />
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  particleCount?: number
  spread?: number
  colors?: string[]
}>(), {
  particleCount: 120,
  spread: 80,
  colors: () => ['#6366f1', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444', '#ec4899', '#06b6d4'],
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
let particles: any[] = []
let animId = 0
let running = false

function fire() {
  if (!canvasRef.value) return
  const canvas = canvasRef.value
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  canvas.style.pointerEvents = 'auto'

  particles = []
  const cx = canvas.width / 2
  const cy = canvas.height / 2

  for (let i = 0; i < props.particleCount; i++) {
    const angle = (Math.PI * 2 * i) / props.particleCount + (Math.random() - 0.5) * 0.5
    const speed = 6 + Math.random() * 18
    particles.push({
      x: cx, y: cy,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - Math.random() * 6,
      size: 6 + Math.random() * 10,
      color: props.colors[Math.floor(Math.random() * props.colors.length)],
      life: 0, maxLife: 60 + Math.random() * 60,
      rotation: Math.random() * 360,
      rotSpeed: (Math.random() - 0.5) * 15,
      shape: Math.random() > 0.5 ? 'rect' : 'circle',
    })
  }

  if (!running) { running = true; animate() }
}

function animate() {
  if (!canvasRef.value) { running = false; return }
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  let alive = false
  for (const p of particles) {
    p.life++
    if (p.life > p.maxLife) continue
    alive = true
    p.vy += 0.15 // gravity
    p.vx *= 0.99 // drag
    p.x += p.vx
    p.y += p.vy
    p.rotation += p.rotSpeed

    const alpha = 1 - p.life / p.maxLife
    ctx.save()
    ctx.translate(p.x, p.y)
    ctx.rotate((p.rotation * Math.PI) / 180)
    ctx.globalAlpha = alpha
    ctx.fillStyle = p.color
    if (p.shape === 'rect') {
      ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6)
    } else {
      ctx.beginPath(); ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2); ctx.fill()
    }
    ctx.restore()
  }

  if (alive) {
    animId = requestAnimationFrame(animate)
  } else {
    running = false
    canvas.style.pointerEvents = 'none'
  }
}

onUnmounted(() => { if (animId) cancelAnimationFrame(animId) })

defineExpose({ fire })
</script>

<style scoped>
.confetti-canvas {
  position: fixed; top: 0; left: 0; z-index: 9999;
  pointer-events: none; width: 100%; height: 100%;
}
</style>
