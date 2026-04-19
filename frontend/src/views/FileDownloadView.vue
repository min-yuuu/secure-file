<template>
  <div class="download-page">
    <div class="download-card">
      <div class="card-header">
        <h2 class="title">文件下载</h2>
        <button @click="goBack" class="btn-back">返回</button>
      </div>

      <form @submit.prevent="handleDownload" class="download-form">
        <div class="form-item">
          <label>文件ID *</label>
          <input 
            v-model="form.fileId" 
            placeholder="请输入文件ID" 
            class="input"
            required
          />
        </div>

        <div class="form-item">
          <label>提取码</label>
          <input 
            v-model="form.extractCode" 
            placeholder="如有提取码请填写，否则留空" 
            class="input"
            type="password"
          />
          <div v-if="form.extractCode" class="hint">
            <span class="tag-info">已设置提取码</span>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="downloading" class="btn-primary">
            {{ downloading ? '下载中...' : '开始下载' }}
          </button>
          <button type="button" @click="resetForm" class="btn-secondary">重置</button>
        </div>
      </form>

      <div v-if="route.query.fileId" class="auto-fill-tip">
        <div class="alert-info">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          <span>检测到文件ID: {{ route.query.fileId }}</span>
          <button @click="fillFromQuery" class="link-btn">点击使用</button>
        </div>
      </div>

      <div class="download-tip" v-if="!downloading">
        <div class="alert-warning">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
          </svg>
          <span>下载说明：如果文件设置了提取码，必须填写正确的提取码才能下载</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useFileDownload } from '../hooks/useFileDownload';

const route = useRoute();
const router = useRouter();
const { startDownload } = useFileDownload();

const downloading = ref(false);

const form = reactive({
  fileId: '',
  extractCode: ''
});

onMounted(() => {
  if (route.query.fileId) {
    form.fileId = route.query.fileId as string;
  }
  if (route.query.extractCode) {
    form.extractCode = route.query.extractCode as string;
  }
});

const fillFromQuery = () => {
  form.fileId = route.query.fileId as string;
  if (route.query.extractCode) {
    form.extractCode = route.query.extractCode as string;
  }
};

const handleDownload = async () => {
  if (!form.fileId || form.fileId.length < 8) {
    ElMessage.warning('请输入有效的文件ID');
    return;
  }

  downloading.value = true;
  try {
    const success = await startDownload(form.fileId, form.extractCode || undefined);
    if (success) {
      ElMessage.success('下载任务已添加到队列');
    }
  } finally {
    downloading.value = false;
  }
};

const resetForm = () => {
  form.fileId = '';
  form.extractCode = '';
};

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.download-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px);
}

.download-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 191, 255, 0.2);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #00bfff;
  text-shadow: 0 0 20px rgba(0, 191, 255, 0.5);
  margin: 0;
}

.btn-back {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.btn-back:hover {
  background: rgba(100, 116, 139, 0.3);
  border-color: rgba(100, 116, 139, 0.5);
}

.download-form {
  margin-bottom: 24px;
}

.form-item {
  margin-bottom: 24px;
}

.form-item label {
  display: block;
  color: #00bfff;
  font-size: 14px;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 191, 255, 0.3);
}

.input {
  width: 100%;
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(0, 191, 255, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  color: #e2e8f0;
  font-size: 14px;
  transition: all 0.3s;
}

.input:focus {
  outline: none;
  border-color: #00bfff;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.2);
}

.hint {
  margin-top: 8px;
}

.tag-info {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  background: rgba(0, 255, 157, 0.2);
  color: #00ff9d;
  border: 1px solid rgba(0, 255, 157, 0.4);
}

.form-actions {
  display: flex;
  gap: 16px;
}

.btn-primary, .btn-secondary {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #00bfff, #00ff9d);
  color: #0a0e1a;
  box-shadow: 0 0 20px rgba(0, 191, 255, 0.3);
}

.btn-primary:hover:not(:disabled) {
  box-shadow: 0 0 30px rgba(0, 191, 255, 0.5);
  transform: translateY(-2px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.btn-secondary:hover {
  background: rgba(100, 116, 139, 0.3);
  border-color: rgba(100, 116, 139, 0.5);
}

.auto-fill-tip, .download-tip {
  margin-top: 24px;
}

.alert-info, .alert-warning {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  font-size: 14px;
}

.alert-info {
  background: rgba(0, 255, 157, 0.1);
  border: 1px solid rgba(0, 255, 157, 0.3);
  color: #00ff9d;
}

.alert-warning {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #f59e0b;
}

.link-btn {
  background: transparent;
  border: none;
  color: #00ff9d;
  text-decoration: underline;
  cursor: pointer;
  font-weight: 600;
}

.link-btn:hover {
  color: #00bfff;
}
</style>
