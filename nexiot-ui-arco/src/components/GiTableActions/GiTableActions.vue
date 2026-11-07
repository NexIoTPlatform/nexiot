<template>
  <span v-if="!availableActions.length">-</span>
  <div v-else class="gi-table-actions">
    <template v-for="(action, index) in displayedActions" :key="action?.key || `more-${index}`">
      <a-dropdown v-if="!action" trigger="click">
        <a-tooltip content="更多操作">
          <a-button type="text" shape="circle" aria-label="更多操作"><template #icon><IconMore /></template></a-button>
        </a-tooltip>
        <template #content>
          <a-doption v-for="item in overflowActions" :key="item.key" :disabled="item.disabled" @click="runOverflow(item)">
            <template #icon><component :is="icons[item.icon]" /></template>
            {{ item.label }}
          </a-doption>
        </template>
      </a-dropdown>
      <a-popconfirm v-else-if="action.confirm" :content="action.confirm" :disabled="action.disabled" :on-before-ok="() => run(action)">
        <a-tooltip :content="action.label">
          <a-button type="text" shape="circle" :status="action.status" :disabled="action.disabled" :aria-label="action.label">
            <template #icon><component :is="icons[action.icon]" /></template>
          </a-button>
        </a-tooltip>
      </a-popconfirm>
      <a-tooltip v-else :content="action.label">
        <a-button type="text" shape="circle" :status="action.status" :disabled="action.disabled" :aria-label="action.label"
          :href="action.href" :target="action.href ? '_blank' : undefined" :rel="action.href ? 'noopener noreferrer' : undefined" @click="run(action)">
          <template #icon><component :is="icons[action.icon]" /></template>
        </a-button>
      </a-tooltip>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { TableAction } from './type'
import { Modal } from '@arco-design/web-vue'
import { IconClose, IconCopy, IconDelete, IconEdit, IconEye, IconHistory, IconLaunch, IconLink, IconLock, IconMore, IconPlayArrow, IconPlus, IconRefresh, IconSafe, IconStorage, IconUndo, IconUpload, IconUserGroup } from '@arco-design/web-vue/es/icon'

defineOptions({ name: 'GiTableActions' })
const props = defineProps<{ actions: TableAction[] }>()
const icons = {
  view: IconEye,
  edit: IconEdit,
  delete: IconDelete,
  add: IconPlus,
  lock: IconLock,
  safe: IconSafe,
  users: IconUserGroup,
  storage: IconStorage,
  apply: IconLaunch,
  publish: IconUpload,
  withdraw: IconUndo,
  play: IconPlayArrow,
  stop: IconClose,
  history: IconHistory,
  retry: IconRefresh,
  copy: IconCopy,
  unlink: IconLink
}
const availableActions = computed(() => props.actions.filter((action) => action.visible !== false))
const folded = computed(() => availableActions.value.length > 5)
// 超过 5 项时保留前三项和最后一项（通常为删除），中间操作折叠。
const displayedActions = computed(() => folded.value ? [...availableActions.value.slice(0, 3), null, ...availableActions.value.slice(-1)] : availableActions.value)
const overflowActions = computed(() => folded.value ? availableActions.value.slice(3, -1) : [])

async function run(action: TableAction) {
  if (action.disabled) return false
  return await action.onClick?.() !== false
}

function runOverflow(action: TableAction) {
  if (action.disabled) return
  if (action.confirm) {
    Modal.warning({ title: action.label, content: action.confirm, hideCancel: false, onBeforeOk: () => run(action) })
  } else if (action.href) {
    window.open(action.href, '_blank', 'noopener,noreferrer')
  } else {
    return run(action)
  }
}
</script>

<style lang="scss" scoped>
.gi-table-actions {
  display: flex;
  gap: 2px;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}
</style>
