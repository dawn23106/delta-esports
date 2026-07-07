<template>
  <div class="dock">
    <div class="dock-inner">
      <div class="dock-item" v-for="item in items" :key="item.path"
        :class="{ active: currentPath === item.path }" @click="go(item.path)">
        <span class="dock-icon">{{ item.icon }}</span>
        <span class="dock-label">{{ item.label }}</span>
        <span v-if="item.badge" class="dock-badge">{{ item.badge }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

defineProps<{
  items: { icon: string; label: string; path: string; badge?: number }[]
}>()

const router = useRouter()
const route = useRoute()
const currentPath = computed(() => route.path)
function go(path: string) { router.push(path) }
</script>

<style scoped>
.dock {
  position: fixed; bottom: 16px; left: 50%; transform: translateX(-50%);
  z-index: 100; padding: 0 4px;
}
.dock-inner {
  display: flex; gap: 6px; align-items: flex-end;
  background: rgba(26,26,26,.85); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 18px; padding: 6px 10px;
  box-shadow: 0 2px 20px rgba(0,0,0,.5);
}
.dock-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 6px 14px; border-radius: 14px; cursor: pointer;
  transition: all .2s; position: relative;
  color: rgba(255,255,255,.45);
}
.dock-item:hover { background: rgba(255,255,255,.06); color: rgba(255,255,255,.8); }
.dock-item.active { background: rgba(135,254,13,.1); color: #87fe0d; }
.dock-icon { font-size: 22px; transition: transform .2s; }
.dock-item:hover .dock-icon { transform: scale(1.15); }
.dock-item.active .dock-icon { transform: scale(1.1); }
.dock-label { font-size: 10px; white-space: nowrap; }
.dock-badge {
  position: absolute; top: 2px; right: 6px;
  background: #e05a5a; color: #fff; font-size: 10px;
  min-width: 16px; height: 16px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
</style>
