<template>
  <div>
    <router-view />
    <van-tabbar v-model="active" @change="onTab" route>
      <van-tabbar-item to="/booster/pool" icon="search">订单池</van-tabbar-item>
      <van-tabbar-item to="/booster/orders" icon="todo-list-o">我接的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const active = ref(0)
function onTab(idx: number) {
  router.push(idx === 0 ? '/booster/pool' : '/booster/orders')
}
</script>
