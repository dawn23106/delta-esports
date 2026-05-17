<template>
  <div>
    <router-view />
    <van-tabbar v-model="active" @change="onTab" route>
      <van-tabbar-item to="/player/home" icon="add-o">下单</van-tabbar-item>
      <van-tabbar-item to="/player/orders" icon="orders-o">我的订单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const active = ref(0)
function onTab(idx: number) {
  router.push(idx === 0 ? '/player/home' : '/player/orders')
}
</script>
