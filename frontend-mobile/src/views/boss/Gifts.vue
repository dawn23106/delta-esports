<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'

const gifts = ref<any[]>([])

async function load() {
  try { const res: any = await request.get('/gifts/sent'); gifts.value = res || [] } catch { }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <van-nav-bar title="我的礼物" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="px-4 mt-3 space-y-2">
      <div v-for="g in gifts" :key="g.id" class="bg-white rounded-xl shadow-sm p-4">
        <div class="flex justify-between"><span class="font-medium">{{ g.giftName }}</span><span class="text-amber-500 font-bold">¥{{ g.amount }}</span></div>
        <div class="text-xs text-gray-400 mt-1">{{ g.message || '无留言' }}</div>
        <div class="text-xs text-gray-300 mt-1">{{ g.createdAt?.replace('T', ' ').substring(0, 16) }}</div>
      </div>
      <div v-if="gifts.length === 0" class="text-center py-20 text-gray-400">暂无送礼记录</div>
    </div>
  </div>
</template>
