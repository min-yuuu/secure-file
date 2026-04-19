<template>
  <div class="file-table">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 表格 -->
    <el-table
        v-loading="loading"
        :data="fileList"
        border
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
    >
      <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="file-name" :title="row.fileName">
            {{ row.fileName }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="fileSize" label="大小" width="120" sortable="custom">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="上传时间" width="160" sortable="custom">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <!-- 直接使用后端返回的过期时间 -->
      <el-table-column prop="expiryTime" label="过期时间" width="220">
        <template #default="{ row }">
          <!-- 有过期时间的情况 -->
          <div v-if="row.expiryTime">
            <div :class="{ expired: isExpired(row.expiryTime) }">
              <div>{{ formatDateTime(row.expiryTime) }}</div>
              <div class="time-info">
                <span :class="getRemainingTimeClass(row.expiryTime)">
                  {{ getRemainingTimeText(row.expiryTime) }}
                </span>
                <el-tag v-if="!isExpired(row.expiryTime)" size="small" :type="getExpiryTagType(row.expiryTime)" effect="plain">
                  {{ getExpiryStatus(row.expiryTime) }}
                </el-tag>
              </div>
            </div>
          </div>
          <!-- 永久有效的情况 -->
          <div v-else class="never-expire">
            <el-tag size="small" type="success" effect="plain">永久有效</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="downloadCount" label="下载次数" width="100" align="center">
        <template #default="{ row }">
          {{ row.downloadCount || 0 }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row.fileId)">
            详情
          </el-button>
          <el-button
              link
              type="success"
              size="small"
              @click="downloadFile(row)"
              :disabled="row.status !== 'COMPLETE' || (row.expiryTime && isExpired(row.expiryTime))"
          >
            下载
          </el-button>
          <el-button
              v-if="props.type === 'uploaded'"
              link
              type="danger"
              size="small"
              @click="deleteFile(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态提示 -->
    <el-empty v-if="!loading && fileList.length === 0" description="暂无文件" :image-size="100" />

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { formatFileSize, formatDateTime } from '../utils/fileUtils';
import type { FileInfo } from '../types/download';

const props = defineProps<{
  type: 'uploaded' | 'received';
}>();

const emit = defineEmits<{
  (e: 'viewDetail', fileId: string): void;
}>();

const router = useRouter();

const loading = ref(false);
const fileList = ref<FileInfo[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const searchKeyword = ref('');
const sortField = ref<string>();
const sortOrder = ref<string>();

// 暴露方法给父组件
defineExpose({
  refresh: () => {
    currentPage.value = 1;
    fetchFiles();
  }
});

// 监听类型变化
watch(() => props.type, () => {
  currentPage.value = 1;
  fetchFiles();
});

onMounted(() => {
  fetchFiles();
});

const fetchFiles = async () => {
  loading.value = true;
  try {
    const params: any = {
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    };

    if (props.type === 'received') {
      params.received = true;
    }

    if (sortField.value && sortOrder.value) {
      params.sortField = sortField.value;
      params.sortOrder = sortOrder.value === 'ascending' ? 'asc' : 'desc';
    }

    const res = await request.get('/api/v1/file/page', { params });
    console.log('文件列表响应:', res.data);

    const responseData = res.data.data || res.data;
    fileList.value = responseData.list || [];
    total.value = responseData.total || 0;

    if (fileList.value.length === 0) {
      console.log('暂无文件数据');
    } else {
      console.log(`加载到 ${fileList.value.length} 个文件`);
      console.log('第一个文件的过期时间:', fileList.value[0]?.expiryTime);
    }
  } catch (error) {
    console.error('获取文件列表失败:', error);
    ElMessage.error('获取文件列表失败');
    fileList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchFiles();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  fetchFiles();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  fetchFiles();
};

const handleSortChange = ({ prop, order }: any) => {
  sortField.value = prop;
  sortOrder.value = order;
  fetchFiles();
};

const getStatusType = (status: string): string => {
  switch (status) {
    case 'COMPLETE': return 'success';
    case 'UPLOADING': return 'warning';
    case 'DELETED': return 'danger';
    default: return 'info';
  }
};

const getStatusText = (status: string): string => {
  switch (status) {
    case 'COMPLETE': return '已完成';
    case 'UPLOADING': return '上传中';
    case 'DELETED': return '已删除';
    default: return status;
  }
};

// ========== 过期时间相关函数（直接使用后端返回的 expiryTime）==========
// 判断是否过期
const isExpired = (expiryTime?: string): boolean => {
  if (!expiryTime) return false; // 没有过期时间（永久有效）不算过期
  try {
    return new Date(expiryTime) < new Date();
  } catch {
    return false;
  }
};

// 获取剩余时间文本
const getRemainingTimeText = (expiryTime: string): string => {
  if (!expiryTime) return '永久有效';
  try {
    const expiry = new Date(expiryTime).getTime();
    const now = new Date().getTime();

    if (expiry <= now) return '已过期';

    const diffMs = expiry - now;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffDays > 0) {
      return `剩余 ${diffDays} 天 ${diffHours} 小时`;
    } else if (diffHours > 0) {
      return `剩余 ${diffHours} 小时 ${diffMinutes} 分钟`;
    } else {
      return `剩余 ${diffMinutes} 分钟`;
    }
  } catch {
    return '';
  }
};

// 获取剩余时间的样式类
const getRemainingTimeClass = (expiryTime: string): string => {
  if (!expiryTime) return '';
  try {
    const expiry = new Date(expiryTime).getTime();
    const now = new Date().getTime();

    if (expiry <= now) return 'expired-text';

    const diffHours = (expiry - now) / (1000 * 60 * 60);
    if (diffHours < 1) {
      return 'urgent-remaining';
    } else if (diffHours < 6) {
      return 'warning-remaining';
    } else if (diffHours < 24) {
      return 'normal-remaining';
    } else {
      return 'long-remaining';
    }
  } catch {
    return '';
  }
};

// 获取过期状态标签类型
const getExpiryTagType = (expiryTime: string): string => {
  if (!expiryTime) return 'success';
  try {
    const expiry = new Date(expiryTime).getTime();
    const now = new Date().getTime();

    if (expiry <= now) return 'danger';

    const diffHours = (expiry - now) / (1000 * 60 * 60);
    if (diffHours < 6) {
      return 'warning';
    } else {
      return 'info';
    }
  } catch {
    return 'info';
  }
};

// 获取过期状态文本
const getExpiryStatus = (expiryTime: string): string => {
  if (!expiryTime) return '永久';
  try {
    const expiry = new Date(expiryTime).getTime();
    const now = new Date().getTime();

    if (expiry <= now) return '已过期';

    const diffHours = (expiry - now) / (1000 * 60 * 60);
    if (diffHours < 6) {
      return '即将过期';
    } else {
      return '有效';
    }
  } catch {
    return '有效';
  }
};

const viewDetail = (fileId: string) => {
  emit('viewDetail', fileId);
};

const downloadFile = (file: FileInfo) => {
  if (file.status !== 'COMPLETE') {
    ElMessage.warning('文件未完成上传，无法下载');
    return;
  }

  if (file.expiryTime && isExpired(file.expiryTime)) {
    ElMessage.error('文件已过期，无法下载');
    return;
  }

  router.push({
    path: '/file-download',
    query: {
      fileId: file.fileId,
      extractCode: file.extractCode || ''
    }
  });
};

const deleteFile = async (file: FileInfo) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除文件 "${file.fileName}" 吗？此操作不可恢复。`,
        '删除确认',
        { type: 'warning' }
    );

    await request.post('/api/v1/file/delete', null, {
      params: { fileId: file.fileId }
    });

    ElMessage.success('删除成功');
    fetchFiles();
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};
</script>

<style scoped>
.file-table {
  padding: 4px;
  min-height: 300px;
}

.search-bar {
  margin-bottom: 16px;
}

.file-name {
  color: #303133;
  font-weight: 500;
}

.expired {
  color: #f56c6c;
  font-weight: bold;
}

.expired-text {
  color: #f56c6c;
  font-size: 12px;
}

.urgent-remaining {
  color: #f56c6c;
  font-weight: bold;
  font-size: 12px;
}

.warning-remaining {
  color: #e6a23c;
  font-size: 12px;
}

.normal-remaining {
  color: #409eff;
  font-size: 12px;
}

.long-remaining {
  color: #67c23a;
  font-size: 12px;
}

.never-expire {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.time-info {
  font-size: 12px;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>