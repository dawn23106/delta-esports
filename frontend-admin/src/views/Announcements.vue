<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const announcements = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<any>({ title: '', content: '', coverImage: '', sortOrder: 0, isActive: true })

async function load() {
  loading.value = true
  try {
    // 后端没有专门的公告接口，通过公开查询
    const res: any = await request.get('/announcements')
    announcements.value = res || []
  } catch {
    announcements.value = []
  } finally { loading.value = false }
}

function openCreate() {
  isEdit.value = false
  form.value = { title: '', content: '', coverImage: '', sortOrder: 0, isActive: true }
  dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await request.put(`/announcements/${form.value.id}`, form.value)
    } else {
      await request.post('/announcements', form.value)
    }
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '保存失败') }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4"><el-button type="primary" @click="openCreate">+ 新增公告</el-button></div>
    <el-table :data="announcements" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.isActive ? 'success' : 'info'" size="small">{{ row.isActive ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default><el-button size="small">编辑</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" rows="4" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverImage" placeholder="图片URL" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
