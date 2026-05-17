<template>
  <div class="page">
    <h3>用户管理</h3>
    <el-table :data="users" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}"><el-tag :type="row.role==='cs'?'danger':row.role==='booster'?'warning':''">{{ row.role }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="small" v-if="row.role==='player'" type="warning" @click="setRole(row.id,'booster')">设为打手</el-button>
          <el-button size="small" v-if="row.role==='booster'" type="info" @click="setRole(row.id,'player')">取消打手</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="20" layout="prev,pager,next" @current-change="load" style="margin-top:16px;justify-content:center" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUsers, updateUserRole } from '../api/admin'
import { ElMessage } from 'element-plus'

const users = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await getUsers(page.value, 20)
    users.value = res.data.list
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(load)

async function setRole(id: number, role: string) {
  await updateUserRole(id, role)
  ElMessage.success('角色已更新')
  load()
}
</script>
