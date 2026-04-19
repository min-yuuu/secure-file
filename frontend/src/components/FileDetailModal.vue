<template>
  <el-dialog
      v-model="visible"
      title="文件详情"
      width="700px"
      destroy-on-close
      @close="handleClose"
  >
    <div v-loading="loading" class="file-detail-modal">
      <!-- 基础信息 -->
      <el-descriptions v-if="fileDetail" :column="2" border>
        <el-descriptions-item label="文件ID">
          <span class="file-id">{{ fileDetail.fileId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="文件名">{{ fileDetail.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(fileDetail.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="上传者ID">{{ fileDetail.userId }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatDateTime(fileDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(fileDetail.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(fileDetail.status)">{{ fileDetail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="MIME类型">{{ fileDetail.contentType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">
          <span :class="{ expired: isExpired(fileDetail.expiryTime) }">
            {{ formatDateTime(fileDetail.expiryTime) || '永久' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="下载次数">
          {{ fileDetail.downloadCount || 0 }} / {{ fileDetail.downloadLimit === -1 ? '无限' : fileDetail.downloadLimit }}
        </el-descriptions-item>
        <el-descriptions-item label="提取码">
          <span v-if="fileDetail.extractCode">已设置</span>
          <span v-else>未设置</span>
        </el-descriptions-item>
        <el-descriptions-item label="阅后即焚">
          <el-tag :type="fileDetail.burnAfterReading ? 'danger' : 'info'" size="small">
            {{ fileDetail.burnAfterReading ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SHA256" :span="2">
          <span class="hash">{{ fileDetail.plainSha256 || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 接收者列表 -->
      <el-divider>
        <span class="divider-text">接收者列表 ({{ recipients.length }})</span>
      </el-divider>

      <el-table :data="recipients" border stripe size="small" v-if="recipients.length > 0">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="wrappedAesKey" label="封装的AES密钥" min-width="250">
          <template #default="{ row }">
            <span class="hash">{{ row.wrappedAesKey?.substring(0, 20) }}...</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="添加时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无接收者" :image-size="60" />

      <!-- 下载记录 -->
      <el-divider>
        <span class="divider-text">下载记录 ({{ downloadRecords.length }})</span>
      </el-divider>

      <el-table :data="downloadRecords" border stripe size="small" v-if="downloadRecords.length > 0">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="下载用户" min-width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="createTime" label="下载时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无下载记录" :image-size="60" />
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../utils/request';
import { formatFileSize, formatDateTime } from '../utils/fileUtils';
import type { FileDetail } from '../types/download';

const props = defineProps<{
  modelValue: boolean;
  fileId?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const visible = ref(props.modelValue);
const loading = ref(false);
const fileDetail = ref<FileDetail | null>(null);
const recipients = ref<any[]>([]);
const downloadRecords = ref<any[]>([]);

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val && props.fileId) {
    loadFileDetail(props.fileId);
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
  if (!val) {
    // 关闭时重置数据
    fileDetail.value = null;
    recipients.value = [];
    downloadRecords.value = [];
  }
});

const getStatusType = (status?: string): string => {
  switch (status) {
    case 'COMPLETE': return 'success';
    case 'UPLOADING': return 'warning';
    case 'DELETED': return 'danger';
    default: return 'info';
  }
};

const isExpired = (expiryTime?: string): boolean => {
  if (!expiryTime) return false;
  return new Date(expiryTime) < new Date();
};

const loadFileDetail = async (fileId: string) => {
  loading.value = true;
  try {
    // 获取文件详情
    const detailRes = await request.get('/api/v1/file/detail', {
      params: { fileId }
    });
    console.log('文件详情响应:', detailRes.data);
    fileDetail.value = detailRes.data.data;

    // 获取接收者列表（需要额外接口，暂时为空）
    recipients.value = [];

    // 获取下载记录（从审计日志筛选）
    const logsRes = await request.get('/api/v1/audit/transfer/page', {
      params: {
        current: 1,
        size: 100,
        action: 'DOWNLOAD',
        fileId
      }
    });
    console.log('下载记录响应:', logsRes.data);
    downloadRecords.value = logsRes.data.data?.list || [];

  } catch (error) {
    console.error('加载文件详情失败:', error);
    ElMessage.error('加载文件详情失败');
  } finally {
    loading.value = false;
  }
};

const handleClose = () => {
  visible.value = false;
};
</script>

<style scoped>
.file-detail-modal {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0 4px;
}

.file-id, .hash {
  font-family: monospace;
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 4px;
  border-radius: 4px;
  word-break: break-all;
}

.expired {
  color: #f56c6c;
  font-weight: bold;
}

.divider-text {
  font-weight: 600;
  color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>