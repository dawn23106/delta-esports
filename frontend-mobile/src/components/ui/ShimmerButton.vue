<template>
  <button class="shimmer-btn" :class="{ 'shimmer-active': active }" @mouseenter="active = true" @mouseleave="active = false">
    <span class="shimmer-content">
      <span class="shimmer-shine" />
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ color?: string }>()
const active = ref(false)
</script>

<style scoped>
.shimmer-btn {
  position: relative; overflow: hidden; border: none; cursor: pointer;
  padding: 12px 28px; border-radius: 14px; background: #6366f1;
  color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 1px;
  transition: all .3s ease;
}
.shimmer-btn:hover { transform: translateY(-1px); }
.shimmer-content { position: relative; z-index: 1; display: flex; align-items: center; gap: 6px; }
.shimmer-shine {
  position: absolute; top: 0; left: -100%; width: 60%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,.25), transparent);
  transition: left .6s ease;
}
.shimmer-active .shimmer-shine { left: 150%; }
</style>
