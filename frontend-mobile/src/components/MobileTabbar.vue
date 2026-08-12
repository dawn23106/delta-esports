<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  role: 'boss' | 'booster'
}>()

const active = computed({
  get: () => 0,
  set: () => undefined,
})

const items = computed(() => props.role === 'boss'
  ? [
      { icon: 'home-o', label: '首页', to: '/boss/home' },
      { icon: 'friends-o', label: '陪玩', to: '/boss/choose' },
      { icon: 'chat-o', label: '消息', to: '/boss/messages' },
      { icon: 'user-o', label: '我的', to: '/boss/profile' },
    ]
  : [
      { icon: 'orders-o', label: '订单池', to: '/booster/pool' },
      { icon: 'logistics', label: '进行中', to: '/booster/orders' },
      { icon: 'chat-o', label: '消息', to: '/booster/messages' },
      { icon: 'user-o', label: '我的', to: '/booster/profile' },
    ])
</script>

<template>
  <van-tabbar
    v-model="active"
    route
    :border="false"
    active-color="#3157ff"
    inactive-color="#98a2b3"
    safe-area-inset-bottom
    class="bottom-tabbar"
  >
    <van-tabbar-item v-for="item in items" :key="item.to" :icon="item.icon" :to="item.to">
      {{ item.label }}
    </van-tabbar-item>
  </van-tabbar>
</template>
