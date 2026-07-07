<template>
  <div class="slider-wrap" @mouseenter="pause" @mouseleave="resume">
    <div class="slider-track" :style="trackStyle">
      <div v-for="(img, i) in images" :key="i" class="slide">
        <img :src="img" class="slide-img" />
      </div>
    </div>
    <div class="slide-overlay" />
    <div class="slide-content">
      <slot />
    </div>
    <div class="slide-dots">
      <span v-for="(_, i) in images" :key="i" :class="['dot', { active: i === current }]" @click="goTo(i)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  images: string[]
  autoplay?: boolean
  interval?: number
}>(), { autoplay: true, interval: 4000 })

const current = ref(0)
let timer: any = null

const trackStyle = computed(() => ({
  transform: `translateX(-${current.value * 100}%)`,
}))

function next() {
  current.value = (current.value + 1) % props.images.length
}
function goTo(i: number) { current.value = i }
function pause() { if (timer) clearInterval(timer) }
function resume() {
  if (props.autoplay) timer = setInterval(next, props.interval)
}

onMounted(() => { if (props.autoplay) timer = setInterval(next, props.interval) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.slider-wrap {
  position: relative; width: 100%; height: 200px; overflow: hidden; border-radius: 12px;
  contain: paint; transform: translateZ(0);
}
.slider-track { display: flex; height: 100%; transition: transform .6s cubic-bezier(.4,0,.2,1); }
.slide { flex-shrink: 0; width: 100%; height: 100%; }
.slide-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.slide-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,.7) 0%, transparent 50%, transparent 100%);
}
.slide-content { position: absolute; bottom: 0; left: 0; right: 0; z-index: 2; }
.slide-dots {
  position: absolute; bottom: 10px; right: 12px; display: flex; gap: 6px; z-index: 3;
}
.dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,.4); cursor: pointer; transition: all .3s; }
.dot.active { background: #fff; width: 18px; border-radius: 3px; }
</style>
