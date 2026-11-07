<template>
  <div class="category-title" tabindex="0" @mouseenter="actionsReady = true" @focusin="actionsReady = true">
    <span class="category-title__name">{{ category.categoryName }}</span>
    <a-tag size="small">L{{ category.level }}</a-tag>
    <a-space v-if="actionsReady" class="category-title__actions" :size="2">
      <a-tooltip v-if="category.level < 3" content="新增子分类">
        <a-button v-hasPerm="['standard:category:add']" type="text" size="mini" shape="circle" aria-label="新增子分类" @click.stop="emit('add', category)"><icon-plus /></a-button>
      </a-tooltip>
      <a-tooltip content="编辑">
        <a-button v-hasPerm="['standard:category:edit']" type="text" size="mini" shape="circle" aria-label="编辑分类" @click.stop="emit('edit', String(category.id))"><icon-edit /></a-button>
      </a-tooltip>
      <a-popconfirm content="确定删除该分类吗？" @before-ok="() => remove(String(category.id))">
        <a-tooltip content="删除">
          <a-button v-hasPerm="['standard:category:remove']" type="text" status="danger" size="mini" shape="circle" aria-label="删除分类" @click.stop><icon-delete /></a-button>
        </a-tooltip>
      </a-popconfirm>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import type { StandardCategoryItem } from '@/apis/iot/standardModel'

defineOptions({ name: 'StandardCategoryTreeTitle' })
defineProps<{
  category: StandardCategoryItem
  remove: (id: string) => Promise<boolean>
}>()
const emit = defineEmits<{
  (e: 'add', category: StandardCategoryItem): void
  (e: 'edit', id: string): void
}>()
// 首次交互后保留操作组件，避免鼠标移向确认框时销毁弹层。
const actionsReady = ref(false)
</script>

<style lang="scss" scoped>
.category-title {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
  min-width: 0;

  &__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__actions {
    opacity: 0;
  }

  &:hover &__actions, &:focus-within &__actions {
    opacity: 1;
  }
}
</style>
