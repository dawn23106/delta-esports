<template>
  <div
    class="upload-zone"
    :class="{ dragging }"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
    @click="openInput"
  >
    <input ref="inputRef" type="file" accept="image/*" multiple hidden @change="onInput" />
    <div v-if="!files.length" class="upload-empty">
      <el-icon size="32" color="#c0c4cc"><component is="Upload" /></el-icon>
      <p>点击或拖拽图片到此处上传</p>
      <span class="upload-hint">支持 JPG / PNG / WebP</span>
    </div>
    <div v-else class="upload-grid">
      <div v-for="(f, i) in files" :key="i" class="upload-item">
        <img :src="f.url" class="upload-thumb" />
        <span class="upload-remove" @click.stop="remove(i)">×</span>
      </div>
      <div class="upload-add" @click.stop="openInput">
        <el-icon size="20" color="#c0c4cc"><component is="Plus" /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits(['update:modelValue'])

const inputRef = ref<HTMLInputElement | null>(null)
const dragging = ref(false)
const files = ref<{ url: string }[]>([])

// 初始化已有值
watch(() => props.modelValue, (val) => {
  if (val && !files.value.length) {
    files.value = [{ url: val }]
  }
}, { immediate: true })

function openInput() { inputRef.value?.click() }
function remove(i: number) { files.value.splice(i, 1); emitValue() }

function processFiles(fileList: FileList) {
  for (const file of fileList) {
    if (!file.type.startsWith('image/')) continue
    const reader = new FileReader()
    reader.onload = () => {
      files.value.push({ url: reader.result as string })
      emitValue()
    }
    reader.readAsDataURL(file)
  }
}

function onInput(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) processFiles(target.files)
}
function onDrop(e: DragEvent) {
  dragging.value = false
  if (e.dataTransfer?.files) processFiles(e.dataTransfer.files)
}

function emitValue() {
  emit('update:modelValue', files.value.map(f => f.url).join(','))
}
</script>

<style scoped>
.upload-zone {
  border: 2px dashed #d9d9d9; border-radius: 10px; padding: 20px;
  text-align: center; cursor: pointer; transition: all .2s;
  background: #fafafa;
}
.upload-zone.dragging { border-color: #6366f1; background: #f0f0ff; }
.upload-empty { padding: 16px 0; }
.upload-empty p { color: #666; font-size: 13px; margin: 8px 0 4px; }
.upload-hint { color: #999; font-size: 11px; }

.upload-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.upload-item { position: relative; aspect-ratio: 1; border-radius: 8px; overflow: hidden; background: #f0f0f0; }
.upload-thumb { width: 100%; height: 100%; object-fit: cover; }
.upload-remove { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; border-radius: 50%; background: rgba(0,0,0,.5); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; cursor: pointer; }
.upload-add { aspect-ratio: 1; border-radius: 8px; border: 1px dashed #d9d9d9; display: flex; align-items: center; justify-content: center; background: #fafafa; }
</style>
