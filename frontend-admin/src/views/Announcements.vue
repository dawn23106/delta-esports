<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const announcements = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<any>({ title: '', content: '', sortOrder: 0, status: 'published' })

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/admin/announcements')
    announcements.value = res.records || []
  } catch {
    announcements.value = []
  } finally { loading.value = false }
}

function openCreate() {
  isEdit.value = false
  form.value = { title: '', content: '', sortOrder: 0, status: 'published' }
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await request.put(`/admin/announcements/${form.value.id}`, form.value)
    } else {
      await request.post('/admin/announcements', form.value)
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
        <template #default="{ row }"><el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">{{ row.status === 'published' ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }"><el-button size="small" @click="openEdit(row)">编辑</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" rows="4" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" active-value="published" inactive-value="draft" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
