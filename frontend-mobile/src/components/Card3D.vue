<template>
  <div class="card-3d-container" @mousemove="onMove" @mouseleave="onLeave">
    <div class="card-3d-body" :style="bodyStyle">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, provide } from 'vue'

const props = withDefaults(defineProps<{ intensity?: number }>(), { intensity: 20 })
const rotateX = ref(0)
const rotateY = ref(0)

const bodyStyle = computed(() => ({
  transform: `perspective(1000px) rotateX(${rotateX.value}deg) rotateY(${rotateY.value}deg)`,
  transition: rotateX.value === 0 && rotateY.value === 0 ? 'transform .5s ease-out' : 'none',
}))

provide('card3dRotateX', rotateX)
provide('card3dRotateY', rotateY)

function onMove(e: MouseEvent) {
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const x = (e.clientY - rect.top) / rect.height - 0.5
  const y = (e.clientX - rect.left) / rect.width - 0.5
  rotateX.value = -x * props.intensity
  rotateY.value = y * props.intensity
}
function onLeave() {
  rotateX.value = 0
  rotateY.value = 0
}
</script>

<style scoped>
.card-3d-container { perspective: 1000px; }
.card-3d-body { transform-style: preserve-3d; }
</style>
