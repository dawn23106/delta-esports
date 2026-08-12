<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'
import FileUpload from '../components/FileUpload.vue'

const services = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<any>({ category: '陪玩专区', name: '', basePrice: 0, priceUnit: 'hour', guaranteeDesc: '', refundPolicy: '', coverImage: '', sortOrder: 0, isActive: true })

async function load() {
  loading.value = true
  try { const res: any = await request.get('/admin/services'); services.value = res || [] } catch { }
  finally { loading.value = false }
}

function openCreate() {
  isEdit.value = false
  form.value = { category: '陪玩专区', name: '', basePrice: 0, priceUnit: 'hour', guaranteeDesc: '', refundPolicy: '', coverImage: '', sortOrder: 0, isActive: true }
  dialogVisible.value = true
}

function openEdit(s: any) {
  isEdit.value = true; form.value = { ...s }; dialogVisible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await request.put(`/services/${form.value.id}`, form.value)
    } else {
      await request.post('/services', form.value)
    }
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '保存失败') }
}

async function toggleActive(s: any) {
  try {
    await request.put(`/services/${s.id}/toggle`, null, { params: { active: !s.isActive } })
    ElMessage.success(s.isActive ? '已下架' : '已上架'); load()
  } catch { }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-4"><el-button type="primary" @click="openCreate">+ 新增服务</el-button></div>
    <el-table :data="services" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="basePrice" label="价格" width="80" />
      <el-table-column prop="priceUnit" label="单位" width="60">
        <template #default="{ row }">{{ row.priceUnit === 'hour' ? '小时' : '局' }}</template>
      </el-table-column>
      <el-table-column prop="guaranteeDesc" label="服务说明" min-width="200" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.isActive ? 'success' : 'info'" size="small">{{ row.isActive ? '上架' : '下架' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.isActive ? 'warning' : 'success'" @click="toggleActive(row)">
            {{ row.isActive ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑服务' : '新增服务'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类"><el-select v-model="form.category"><el-option v-for="c in ['陪玩专区','监狱专区','趣味玩法']" :key="c" :label="c" :value="c" /></el-select></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.basePrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单位"><el-select v-model="form.priceUnit"><el-option label="小时" value="hour" /><el-option label="局" value="round" /></el-select></el-form-item>
        <el-form-item label="服务说明"><el-input v-model="form.guaranteeDesc" /></el-form-item>
        <el-form-item label="退款规则"><el-input v-model="form.refundPolicy" /></el-form-item>
        <el-form-item label="封面图片">
          <FileUpload v-model="form.coverImage" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
