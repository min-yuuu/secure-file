/**
* @description:
* @author:hm
* @date: 2026/2/13
*/

<template>
  <div class="upload-progress">
    <div class="progress-header">
      <span>文件名：{{ file?.name }}</span>
      <span>大小：{{ formatFileSize(file?.size || 0) }}</span>
      <span>进度：{{ progress }}%</span>
    </div>
    <div class="progress-bar">
      <div class="progress-bar-inner" :style="{ width: progress + '%' }"></div>
    </div>
    <div class="chunk-list">
      <div
          v-for="chunk in chunks"
          :key="chunk.index"
          class="chunk-item"
          :class="chunk.status"
      >
        {{ chunk.index }}
      </div>
    </div>
    <div class="actions">
      <el-button v-if="isUploading" @click="pauseUpload">暂停</el-button>
      <el-button v-else @click="resumeUpload">继续</el-button>
      <el-button type="danger" @click="cancelUpload">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChunkInfo } from '../types/upload'

interface Props {
  file: File | null
  chunks: ChunkInfo[]
  progress: number
  isUploading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  pause: []
  resume: []
  cancel: []
}>()

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const pauseUpload = () => emit('pause')
const resumeUpload = () => emit('resume')
const cancelUpload = () => emit('cancel')
</script>

<style scoped>
.upload-progress {
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
}
.progress-header {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}
.progress-bar {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}
.progress-bar-inner {
  height: 100%;
  background: #409eff;
  transition: width 0.3s;
}
.chunk-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}
.chunk-item {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 4px;
  font-size: 12px;
}
.chunk-item.waiting {
  background: #f0f0f0;
  color: #999;
}
.chunk-item.uploading {
  background: #409eff;
  color: #fff;
}
.chunk-item.success {
  background: #67c23a;
  color: #fff;
}
.chunk-item.failed {
  background: #f56c6c;
  color: #fff;
}
.actions {
  display: flex;
  gap: 8px;
}
</style>