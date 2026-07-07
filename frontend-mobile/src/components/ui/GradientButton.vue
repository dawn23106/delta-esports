<template>
  <button
    class="gradient-btn"
    :style="btnStyle"
    @mousemove="onMouseMove"
    @mouseleave="onMouseLeave"
  >
    <span class="gradient-btn-text"><slot /></span>
  </button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  colorFrom?: string
  colorTo?: string
  colorVia?: string
}>(), {
  colorFrom: '#6366f1',
  colorTo: '#8b5cf6',
  colorVia: '#a78bfa',
})

const mouseX = ref(50)
const mouseY = ref(50)

const btnStyle = computed(() => ({
  background: `linear-gradient(${mouseX.value + 45}deg, ${props.colorFrom}, ${props.colorVia || props.colorTo}, ${props.colorTo})`,
}))

function onMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  mouseX.value = ((e.clientX - rect.left) / rect.width) * 100
  mouseY.value = ((e.clientY - rect.top) / rect.height) * 100
}
function onMouseLeave() { mouseX.value = 50; mouseY.value = 50 }
</script>

<style scoped>
.gradient-btn {
  position: relative; overflow: hidden; border: none; cursor: pointer;
  padding: 12px 32px; border-radius: 14px; color: #fff;
  font-size: 15px; font-weight: 700; letter-spacing: 1px;
  transition: transform .2s, box-shadow .2s;
  box-shadow: 0 4px 20px rgba(99,102,241,.3);
}
.gradient-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(99,102,241,.45); }
.gradient-btn:active { transform: scale(.97); }
.gradient-btn-text { position: relative; z-index: 1; }
</style>
