<template>
  <div class="files-page">
    <el-card class="files-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的文件</span>
          <el-button type="primary" @click="refresh">刷新</el-button>
        </div>
      </template>

      <!-- 双标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我上传的" name="uploaded">
          <FileTable
              ref="uploadedTableRef"
              :type="'uploaded'"
              @view-detail="viewDetail"
              @download="goToDownload"
          />
        </el-tab-pane>
        <el-tab-pane label="我接收的" name="received">
          <FileTable
              ref="receivedTableRef"
              :type="'received'"
              @view-detail="viewDetail"
              @download="goToDownload"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 文件详情模态框 -->
    <FileDetailModal
        v-model="detailModalVisible"
        :file-id="currentFileId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import FileTable from '../components/FileTable.vue';
import FileDetailModal from '../components/FileDetailModal.vue';

const router = useRouter();
const activeTab = ref('uploaded');
const uploadedTableRef = ref<InstanceType<typeof FileTable> | null>(null);
const receivedTableRef = ref<InstanceType<typeof FileTable> | null>(null);
const detailModalVisible = ref(false);
const currentFileId = ref<string>();

onMounted(() => {
  // 页面加载时自动刷新上传的文件列表
  setTimeout(() => {
    uploadedTableRef.value?.refresh();
  }, 100);
});

const handleTabChange = () => {
  // 切换标签时刷新对应表格
  if (activeTab.value === 'uploaded') {
    uploadedTableRef.value?.refresh();
  } else {
    receivedTableRef.value?.refresh();
  }
};

const refresh = () => {
  ElMessage.info('刷新中...');
  if (activeTab.value === 'uploaded') {
    uploadedTableRef.value?.refresh();
  } else {
    receivedTableRef.value?.refresh();
  }
};

const viewDetail = (fileId: string) => {
  currentFileId.value = fileId;
  detailModalVisible.value = true;
};

const goToDownload = (fileId: string, extractCode?: string) => {
  router.push({
    path: '/download',
    query: { fileId, extractCode: extractCode || '' }
  });
};
</script>

<style scoped>
.files-page {
  max-width: 1200px;
  margin: 0 auto;
}

.files-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
</style>