<template>
  <div class="download-manager">
    <!-- 悬浮按钮 -->
    <el-badge :value="activeCount" :hidden="activeCount === 0" class="floating-badge">
      <el-button
          type="primary"
          circle
          class="floating-button"
          @click="expanded = !expanded"
      >
        <el-icon size="20">
          <component :is="expanded ? 'ArrowDown' : 'Download'" />
        </el-icon>
      </el-button>
    </el-badge>

    <!-- 展开面板 -->
    <el-card v-show="expanded" class="download-panel" shadow="large">
      <template #header>
        <div class="panel-header">
          <span>下载管理器</span>
          <div class="header-actions">
            <el-button link size="small" @click="clearCompleted">清除已完成</el-button>
            <el-button link size="small" @click="expanded = false">关闭</el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <div class="stats">
        <el-tag size="small" type="info">总数: {{ queueStats.all }}</el-tag>
        <el-tag size="small" type="warning">活跃: {{ queueStats.active }}</el-tag>
        <el-tag size="small" type="success">完成: {{ queueStats.completed }}</el-tag>
        <el-tag size="small" type="danger">失败: {{ queueStats.failed }}</el-tag>
        <el-tag size="small">暂停: {{ queueStats.paused }}</el-tag>
      </div>

      <!-- 任务列表 -->
      <div class="task-list" v-if="tasks.length > 0">
        <div v-for="task in sortedTasks" :key="task.id" class="task-item">
          <div class="task-header">
            <div class="task-title">
              <el-tooltip :content="task.fileName" placement="top">
                <span class="file-name">{{ truncateFileName(task.fileName) }}</span>
              </el-tooltip>
              <el-tag size="small" :type="getStatusTagType(task.status)" class="status-tag">
                {{ getStatusText(task.status) }}
              </el-tag>
            </div>
            <div class="task-actions">
              <el-button
                  v-if="task.status === 'downloading'"
                  link
                  size="small"
                  @click="pauseTask(task.id)"
              >
                <el-icon><VideoPause /></el-icon>
              </el-button>
              <el-button
                  v-else-if="task.status === 'paused'"
                  link
                  size="small"
                  @click="resumeTask(task.id)"
              >
                <el-icon><VideoPlay /></el-icon>
              </el-button>
              <el-button
                  v-if="task.status === 'failed'"
                  link
                  size="small"
                  @click="retryTask(task.id)"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
              <el-button link size="small" @click="cancelTask(task.id)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="progress-area">
            <el-progress
                :percentage="task.progress"
                :status="getProgressStatus(task)"
                :stroke-width="8"
            />
            <div class="progress-info">
              <span>{{ formatFileSize(task.fileSize) }}</span>
              <span>{{ task.completedChunks }}/{{ task.totalChunks }} 分片</span>
            </div>
          </div>

          <!-- 错误信息 -->
          <div v-if="task.error" class="error-message">
            <el-alert :title="task.error" type="error" :closable="false" show-icon />
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无下载任务" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  Download,
  ArrowDown,
  VideoPause,
  VideoPlay,
  Refresh,
  Close
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useFileDownload } from '../hooks/useFileDownload';
import { formatFileSize } from '../utils/fileUtils';
import type { DownloadTask } from '../types/download';

const expanded = ref(false);
const {
  tasks,
  activeCount,
  queueStats,
  pauseTask,
  resumeTask,
  cancelTask,
  retryTask,
  clearCompletedTasks
} = useFileDownload();

// 排序任务：进行中 > 暂停 > 失败 > 等待 > 已完成
const sortedTasks = computed<DownloadTask[]>(() => {
  const statusOrder: Record<string, number> = {
    'downloading': 0,
    'decrypting': 1,
    'applying': 2,
    'paused': 3,
    'failed': 4,
    'pending': 5,
    'completed': 6,
    'cancelled': 7
  };

  return [...tasks.value].sort((a, b) => {
    return (statusOrder[a.status] || 99) - (statusOrder[b.status] || 99);
  });
});

const getStatusTagType = (status: string): string => {
  const map: Record<string, string> = {
    'pending': 'info',
    'applying': 'warning',
    'decrypting': 'warning',
    'downloading': 'primary',
    'paused': '',
    'completed': 'success',
    'failed': 'danger',
    'cancelled': 'info'
  };
  return map[status] || 'info';
};

const getStatusText = (status: string): string => {
  const map: Record<string, string> = {
    'pending': '等待中',
    'applying': '申请中',
    'decrypting': '解密中',
    'downloading': '下载中',
    'paused': '已暂停',
    'completed': '已完成',
    'failed': '失败',
    'cancelled': '已取消'
  };
  return map[status] || status;
};

const getProgressStatus = (task: DownloadTask): 'success' | 'warning' | 'exception' => {
  if (task.status === 'failed') return 'exception';
  if (task.status === 'completed') return 'success';
  return 'warning';
};

const truncateFileName = (name: string, maxLength: number = 30): string => {
  if (name.length <= maxLength) return name;
  return name.substring(0, maxLength - 3) + '...';
};

const clearCompleted = () => {
  clearCompletedTasks();
  ElMessage.success('已清除已完成任务');
};
</script>

<style scoped>
.download-manager {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 9999;
}

.floating-badge {
  position: relative;
}

.floating-button {
  width: 56px;
  height: 56px;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.download-panel {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 400px;
  max-width: calc(100vw - 60px);
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 0 4px;
}

.task-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.task-item {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.status-tag {
  flex-shrink: 0;
}

.task-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.progress-area {
  margin: 8px 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.error-message {
  margin-top: 8px;
}
</style>