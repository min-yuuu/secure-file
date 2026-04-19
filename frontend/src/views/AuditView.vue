<template>
  <div class="audit-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">📊 审计日志</h2>
        <p class="page-desc">Security Audit Logs - 所有文件传输操作记录</p>
      </div>
      <button class="refresh-btn" @click="refresh">
        <span class="btn-icon">🔄</span>
        <span>刷新</span>
      </button>
    </div>

    <!-- 筛选条件 -->
    <div class="filters-section">
      <div class="filter-item">
        <label class="filter-label">操作类型</label>
        <select v-model="filter.action" class="filter-select">
          <option value="">全部</option>
          <option value="UPLOAD">上传</option>
          <option value="DOWNLOAD">下载</option>
        </select>
      </div>

      <div class="filter-item">
        <label class="filter-label">用户ID</label>
        <input 
          v-model="filter.userId" 
          type="text" 
          placeholder="输入用户ID" 
          class="filter-input"
        />
      </div>

      <div class="filter-item filter-date">
        <label class="filter-label">日期范围</label>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-picker"
        />
      </div>

      <button class="search-btn" @click="handleSearch">
        <span class="btn-icon">🔍</span>
        <span>查询</span>
      </button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon upload">⬆️</div>
        <div class="stat-info">
          <div class="stat-value">{{ uploadCount }}</div>
          <div class="stat-label">上传次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon download">⬇️</div>
        <div class="stat-info">
          <div class="stat-value">{{ downloadCount }}</div>
          <div class="stat-label">下载次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ total }}</div>
          <div class="stat-label">总记录数</div>
        </div>
      </div>
    </div>

    <!-- 日志表格 -->
    <div class="table-container">
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <div class="loading-text">加载中...</div>
      </div>

      <table class="log-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>时间</th>
            <th>操作</th>
            <th>文件名</th>
            <th>文件ID</th>
            <th>用户</th>
            <th>用户ID</th>
            <th>IP地址</th>
            <th>用户代理</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logList" :key="log.id" class="log-row">
            <td>{{ log.id }}</td>
            <td>{{ formatDateTime(log.createTime) }}</td>
            <td>
              <span class="action-badge" :class="log.action.toLowerCase()">
                {{ log.action === 'UPLOAD' ? '⬆️ 上传' : '⬇️ 下载' }}
              </span>
            </td>
            <td class="file-name">{{ log.fileName }}</td>
            <td class="file-id">{{ log.fileId }}</td>
            <td>{{ log.username }}</td>
            <td>{{ log.userId }}</td>
            <td class="ip-address">{{ log.ip }}</td>
            <td class="user-agent">{{ log.userAgent }}</td>
          </tr>
          <tr v-if="logList.length === 0 && !loading">
            <td colspan="9" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <div class="empty-text">暂无审计日志</div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1"
        @click="handleCurrentChange(currentPage - 1)"
      >
        ← 上一页
      </button>
      
      <div class="page-info">
        <span class="page-current">{{ currentPage }}</span>
        <span class="page-separator">/</span>
        <span class="page-total">{{ Math.ceil(total / pageSize) }}</span>
      </div>

      <button 
        class="page-btn" 
        :disabled="currentPage >= Math.ceil(total / pageSize)"
        @click="handleCurrentChange(currentPage + 1)"
      >
        下一页 →
      </button>

      <select v-model="pageSize" @change="handleSizeChange" class="page-size-select">
        <option :value="10">10 条/页</option>
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
        <option :value="100">100 条/页</option>
      </select>

      <div class="total-info">
        共 <span class="total-count">{{ total }}</span> 条记录
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../utils/request';
import { formatDateTime } from '../utils/fileUtils';
import type { AuditLog } from '../types/download';

// 筛选条件
const filter = ref({
  action: '',
  userId: ''
});

const dateRange = ref<string[]>([]);

// 表格数据
const loading = ref(false);
const logList = ref<AuditLog[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);

// 统计数据
const uploadCount = computed(() => 
  logList.value.filter(log => log.action === 'UPLOAD').length
);

const downloadCount = computed(() => 
  logList.value.filter(log => log.action === 'DOWNLOAD').length
);

onMounted(() => {
  loadLogs();
});

// 加载日志列表
const loadLogs = async () => {
  loading.value = true;
  try {
    const params: any = {
      current: currentPage.value,
      size: pageSize.value
    };

    if (filter.value.action) {
      params.action = filter.value.action;
    }

    if (filter.value.userId) {
      params.userId = filter.value.userId;
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }

    const res = await request.get('/api/v1/audit/transfer/page', { params });
    const data = res.data.data;

    logList.value = data.list || [];
    total.value = data.total || 0;

    // 补充文件名信息和格式化 IP 地址
    logList.value = logList.value.map(log => ({
      ...log,
      fileName: log.fileName || `文件_${log.fileId}`,
      ip: formatIpAddress(log.ip)
    }));
  } catch (error) {
    console.error('加载日志失败:', error);
    ElMessage.error('加载日志失败');
  } finally {
    loading.value = false;
  }
};

// 格式化 IP 地址
const formatIpAddress = (ip: string | undefined): string => {
  if (!ip) return '-';
  if (ip === '0:0:0:0:0:0:0:1' || ip === '::1') {
    return '127.0.0.1';
  }
  return ip;
};

const handleSearch = () => {
  currentPage.value = 1;
  loadLogs();
};

const refresh = () => {
  filter.value = { action: '', userId: '' };
  dateRange.value = [];
  currentPage.value = 1;
  loadLogs();
};

const handleSizeChange = () => {
  currentPage.value = 1;
  loadLogs();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  loadLogs();
};
</script>

<style scoped>
.audit-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  color: #00bfff;
  margin: 0 0 8px 0;
  text-shadow: 0 0 15px rgba(0, 191, 255, 0.5);
}

.page-desc {
  font-size: 14px;
  color: rgba(0, 255, 157, 0.7);
  margin: 0;
  letter-spacing: 1px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.2), rgba(0, 255, 157, 0.2));
  border: 1px solid rgba(0, 191, 255, 0.5);
  border-radius: 8px;
  color: #00bfff;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.3), rgba(0, 255, 157, 0.3));
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.4);
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 18px;
}

/* 筛选区域 */
.filters-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-date {
  flex: 1;
}

.filter-label {
  font-size: 12px;
  color: rgba(0, 191, 255, 0.8);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.filter-select,
.filter-input {
  padding: 10px 16px;
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
  min-width: 150px;
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.3);
}

.filter-input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  margin-top: auto;
  background: linear-gradient(135deg, rgba(0, 255, 157, 0.3), rgba(0, 191, 255, 0.3));
  border: 1px solid rgba(0, 255, 157, 0.5);
  border-radius: 6px;
  color: #00ff9d;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.search-btn:hover {
  background: linear-gradient(135deg, rgba(0, 255, 157, 0.4), rgba(0, 191, 255, 0.4));
  box-shadow: 0 0 20px rgba(0, 255, 157, 0.4);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(15, 20, 25, 0.8), rgba(10, 14, 26, 0.8));
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 12px;
  transition: all 0.3s;
}

.stat-card:hover {
  border-color: rgba(0, 191, 255, 0.5);
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.2);
  transform: translateY(-2px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  font-size: 28px;
  border-radius: 12px;
  border: 1px solid rgba(0, 191, 255, 0.3);
}

.stat-icon.upload {
  background: linear-gradient(135deg, rgba(0, 191, 255, 0.2), rgba(0, 191, 255, 0.1));
}

.stat-icon.download {
  background: linear-gradient(135deg, rgba(0, 255, 157, 0.2), rgba(0, 255, 157, 0.1));
}

.stat-icon.total {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.2), rgba(255, 107, 107, 0.1));
}

.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: #00bfff;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.5);
  font-family: 'Courier New', monospace;
}

.stat-label {
  font-size: 14px;
  color: rgba(0, 255, 157, 0.7);
}

/* 表格容器 */
.table-container {
  position: relative;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(10, 14, 26, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 10;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(0, 191, 255, 0.2);
  border-top-color: #00bfff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: #00bfff;
  font-size: 16px;
  font-weight: 600;
}

/* 表格样式 */
.log-table {
  width: 100%;
  border-collapse: collapse;
}

.log-table thead {
  background: rgba(0, 191, 255, 0.1);
  border-bottom: 2px solid rgba(0, 191, 255, 0.3);
}

.log-table th {
  padding: 16px 12px;
  text-align: left;
  font-size: 12px;
  font-weight: 700;
  color: #00bfff;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.log-table tbody tr {
  border-bottom: 1px solid rgba(0, 191, 255, 0.1);
  transition: all 0.3s;
}

.log-table tbody tr:hover {
  background: rgba(0, 191, 255, 0.05);
}

.log-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.action-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.action-badge.upload {
  background: rgba(0, 191, 255, 0.2);
  color: #00bfff;
  border: 1px solid rgba(0, 191, 255, 0.3);
}

.action-badge.download {
  background: rgba(0, 255, 157, 0.2);
  color: #00ff9d;
  border: 1px solid rgba(0, 255, 157, 0.3);
}

.file-name {
  color: rgba(0, 255, 157, 0.9);
  font-weight: 500;
}

.file-id,
.user-agent {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ip-address {
  font-family: 'Courier New', monospace;
  color: rgba(0, 191, 255, 0.8);
}

.empty-row {
  padding: 60px 20px !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
}

.page-btn {
  padding: 10px 20px;
  background: rgba(0, 191, 255, 0.1);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 6px;
  color: #00bfff;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: rgba(0, 191, 255, 0.2);
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.3);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(0, 191, 255, 0.1);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 6px;
}

.page-current {
  font-size: 18px;
  font-weight: 800;
  color: #00bfff;
  font-family: 'Courier New', monospace;
}

.page-separator {
  color: rgba(255, 255, 255, 0.3);
}

.page-total {
  color: rgba(0, 255, 157, 0.8);
  font-family: 'Courier New', monospace;
}

.page-size-select {
  padding: 10px 16px;
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
}

.total-info {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.total-count {
  color: #00bfff;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

/* Element Plus 日期选择器深色样式 */
.date-picker :deep(.el-input__wrapper) {
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  box-shadow: none;
}

.date-picker :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

.date-picker :deep(.el-range-separator) {
  color: rgba(255, 255, 255, 0.5);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filters-section {
    flex-direction: column;
  }
}
</style>
