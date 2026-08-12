<template>
  <div class="tilt-container" @mousemove="onMove" @mouseleave="onLeave">
    <div class="tilt-body" :style="bodyStyle">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{ intensity?: number }>(), { intensity: 12 })
const rotateX = ref(0)
const rotateY = ref(0)

const bodyStyle = computed(() => ({
  transform: `perspective(800px) rotateX(${rotateX.value}deg) rotateY(${rotateY.value}deg)`,
  transition: rotateX.value === 0 && rotateY.value === 0 ? 'transform .4s ease' : 'none',
}))

function calcRotate(clientX: number, clientY: number, el: HTMLElement) {
  const rect = el.getBoundingClientRect()
  const x = (clientY - rect.top) / rect.height - 0.5
  const y = (clientX - rect.left) / rect.width - 0.5
  rotateX.value = -x * props.intensity
  rotateY.value = y * props.intensity
}
function onMove(e: MouseEvent) { calcRotate(e.clientX, e.clientY, e.currentTarget as HTMLElement) }
function onLeave() { rotateX.value = 0; rotateY.value = 0 }
</script>

<style scoped>
.tilt-container { perspective: 800px; }
.tilt-body { transform-style: preserve-3d; will-change: transform; }
</style>
