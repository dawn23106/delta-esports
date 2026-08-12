<template>
  <span class="scramble-text" ref="elRef">{{ display }}</span>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = withDefaults(defineProps<{
  text?: string
  speed?: number
  chars?: string
}>(), {
  text: '',
  speed: 50,
  chars: '!@#$%^&*()_+-=[]{}|;:,.<>?/~`ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
})

const elRef = ref<HTMLElement | null>(null)
const display = ref('')
let timer: any = null
let frame = 0
const maxFrames = 15

function scramble() {
  if (!props.text) return
  clearInterval(timer)
  frame = 0
  const target = props.text
  display.value = target

  timer = setInterval(() => {
    frame++
    if (frame >= maxFrames) {
      display.value = target
      clearInterval(timer)
      return
    }
    const progress = frame / maxFrames
    display.value = target
      .split('')
      .map((c, i) => {
        if (c === ' ') return ' '
        if (Math.random() > progress) {
          return props.chars[Math.floor(Math.random() * props.chars.length)]
        }
        return target[i]
      })
      .join('')
  }, props.speed)
}

watch(() => props.text, scramble)
onMounted(() => {
  if (props.text) scramble()
  const observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting) scramble()
  }, { threshold: 0.5 })
  if (elRef.value) observer.observe(elRef.value)
  onUnmounted(() => observer.disconnect())
})
</script>

<style scoped>
.scramble-text { font-family: monospace; white-space: pre-wrap; }
</style>
