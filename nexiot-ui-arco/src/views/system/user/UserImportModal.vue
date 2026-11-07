<template>
  <a-modal v-model:visible="visible" title="导入用户" width="560px" :mask-closable="false" @before-ok="save" @close="close">
    <a-upload v-model:file-list="fileList" draggable :auto-upload="false" :limit="1" accept=".xlsx,.xls">
      <template #upload-button>
        <div class="arco-upload-drag">
          <div class="arco-upload-drag-icon"><icon-upload /></div>
          <div class="arco-upload-drag-text">点击或拖拽 Excel 文件到此处</div>
        </div>
      </template>
    </a-upload>
    <div class="user-import__options">
      <a-checkbox v-model="updateSupport">更新已存在的用户</a-checkbox>
      <a-button type="text" @click="downloadUserImportTemplate">下载导入模板</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { FileItem } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { downloadUserImportTemplate, importUsers } from '@/apis/system/user'

defineOptions({ name: 'UserImportModal' })

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const visible = ref(false)
const fileList = ref<FileItem[]>([])
const updateSupport = ref(false)

function open() {
  fileList.value = []
  updateSupport.value = false
  visible.value = true
}

function close() {
  fileList.value = []
  updateSupport.value = false
}

async function save() {
  const file = fileList.value[0]?.file
  if (!file) {
    Message.warning('请选择需要导入的 Excel 文件')
    return false
  }
  const data = new FormData()
  data.append('file', file)
  data.append('updateSupport', updateSupport.value ? '1' : '0')
  const res = await importUsers(data)
  Message.success(res.message || '用户导入成功')
  emit('save-success')
  return true
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.user-import {
  &__options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
  }
}
</style>
