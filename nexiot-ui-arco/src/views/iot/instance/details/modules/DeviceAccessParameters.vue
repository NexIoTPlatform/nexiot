<template>
  <section v-if="items.length || plugin?.guide" class="device-access">
    <div class="device-access__title">
      <icon-unordered-list class="device-access__title-icon" />
      <span>设备接入参数</span>
      <span class="device-access__title-sub">管理当前设备专属的账号与连接参数</span>
      <a-button v-hasPerm="['system:instance:edit']" class="device-access__edit" type="text" size="small" @click="emit('edit')">
        <template #icon><icon-edit /></template>
        编辑接入参数
      </a-button>
    </div>

    <dl v-if="simpleItems.length" class="device-access__grid">
      <div v-for="item in simpleItems" :key="item.key" class="device-access__field">
        <dt class="device-access__label">
          <a-tooltip :content="item.name">
            <span class="device-access__label-name">{{ item.name }}</span>
          </a-tooltip>
          <a-tooltip v-if="item.name !== item.key" :content="item.key">
            <code class="device-access__label-key">{{ item.key }}</code>
          </a-tooltip>
        </dt>
        <dd class="device-access__value">
          <a-tooltip v-if="!item.secret && item.rawValue" :content="item.displayValue">
            <code class="device-access__code">{{ item.displayValue }}</code>
          </a-tooltip>
          <span v-else class="device-access__text" :class="{ 'is-empty': !item.displayValue || item.displayValue === '-' }">
            {{ item.displayValue || '-' }}
          </span>
          <a-tooltip v-if="item.rawValue && !item.secret" content="复制参数值">
            <a-button type="text" size="mini" class="device-access__icon-btn" :aria-label="`复制${item.name}`" @click="copy(item.rawValue)">
              <template #icon><icon-copy /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="item.description" :content="item.description" position="tl">
            <icon-question-circle class="device-access__help" />
          </a-tooltip>
        </dd>
      </div>
    </dl>

    <div v-if="complexItems.length" class="device-access__extended">
      <div class="device-access__extended-title">扩展信息 <span>{{ complexItems.length }} 项</span></div>
      <dl class="device-access__grid device-access__grid--extended">
        <div v-for="item in complexItems" :key="item.key" class="device-access__field">
          <dt class="device-access__label">
            <a-tooltip :content="item.name">
              <span class="device-access__label-name">{{ item.name }}</span>
            </a-tooltip>
            <a-tooltip v-if="item.name !== item.key" :content="item.key">
              <code class="device-access__label-key">{{ item.key }}</code>
            </a-tooltip>
          </dt>
          <dd class="device-access__value">
            <span class="device-access__preview">{{ item.previewValue }}</span>
            <a-tooltip v-if="item.description" :content="item.description" position="tl">
              <icon-question-circle class="device-access__help" />
            </a-tooltip>
            <a-button type="text" size="small" class="device-access__view" :aria-label="`查看${item.name}完整值`" @click="showDetail(item)">
              查看详情
            </a-button>
            <a-tooltip content="复制完整参数值">
              <a-button type="text" size="mini" class="device-access__icon-btn" :aria-label="`复制${item.name}`" @click="copy(item.rawValue)">
                <template #icon><icon-copy /></template>
              </a-button>
            </a-tooltip>
          </dd>
        </div>
      </dl>
    </div>

    <component :is="plugin.guide" v-if="plugin?.guide" :device="device" :product="product" />

    <a-modal v-model:visible="detailVisible" title="参数详情" width="min(900px, 94vw)" :footer="false" unmount-on-close>
      <template v-if="selected">
        <div class="device-access__detail-heading">
          <div class="device-access__detail-label">
            <strong>{{ selected.name }}</strong>
            <code v-if="selected.name !== selected.key">{{ selected.key }}</code>
          </div>
          <a-button type="outline" size="small" @click="copy(selected.rawValue)">
            <template #icon><icon-copy /></template>
            复制完整值
          </a-button>
        </div>
        <pre class="device-access__detail-code">{{ selected.detailValue }}</pre>
      </template>
    </a-modal>
  </section>
</template>

<script setup lang="ts">
import type { DeviceAccessParameter, DeviceAccessValuePresentation } from './deviceAccessParameters'
import type { DeviceItem } from '@/apis/iot/device'
import type { ProductItem } from '@/apis/iot/product'
import type { DeviceCustomField } from '@/views/iot/instance/modules/deviceCustomFields'
import { Message } from '@arco-design/web-vue'
import { copyText } from '@/utils'
import { buildDeviceAccessParameters, getDeviceAccessValuePresentation } from './deviceAccessParameters'
import { parseRecord } from './deviceCommand'
import { getProtocolDeviceAccess } from './protocolDeviceAccessRegistry'

defineOptions({ name: 'DeviceAccessParameters' })
const props = defineProps<{ device: DeviceItem, product: ProductItem }>()
const emit = defineEmits<{ (e: 'edit'): void }>()
type ParameterView = DeviceAccessParameter & DeviceAccessValuePresentation

const detailVisible = ref(false)
const selectedKey = ref('')
const plugin = computed(() => getProtocolDeviceAccess(props.product.thirdPlatform || props.device.thirdPlatform))
const items = computed(() => {
  const stored = parseRecord(props.device.devConfiguration ?? props.device.configuration)
  const configuration = plugin.value?.normalize?.(stored, props.device) || stored
  const custom = parseRecord(props.product.thirdConfiguration).customField
  const customFields = Array.isArray(custom) ? custom as DeviceCustomField[] : []
  const fields = [...(plugin.value?.fields || []), ...customFields.filter((field) => !plugin.value?.fields.some((known) => known.id === field.id))]
  return buildDeviceAccessParameters(configuration, fields).map((item) => ({ ...item, ...getDeviceAccessValuePresentation(item) }))
})
const simpleItems = computed(() => items.value.filter((item) => !item.expandable))
const complexItems = computed(() => items.value.filter((item) => item.expandable))
const selected = computed(() => complexItems.value.find((item) => item.key === selectedKey.value))

watch(() => props.device.id, () => {
  detailVisible.value = false
  selectedKey.value = ''
})
watch(selected, (item) => {
  if (!item) detailVisible.value = false
})

function showDetail(item: ParameterView) {
  selectedKey.value = item.key
  detailVisible.value = true
}

async function copy(value: string) {
  try {
    await copyText(value)
    Message.success('已复制到剪贴板')
  } catch {
    Message.error('复制失败')
  }
}
</script>

<style scoped lang="scss" src="./DeviceAccessParameters.scss"></style>
