<template>
  <div class="value-type-editor" :class="{ 'value-type-editor--nested': depth > 0 }">
    <a-form-item label="数据类型">
      <a-select :model-value="model.type" :options="dataTypeOptions" @change="changeType" />
    </a-form-item>

    <a-grid v-if="isNumber" :cols="{ xs: 1, md: 2 }" :col-gap="12">
      <a-form-item label="最小值"><a-input-number v-model="model.min" /></a-form-item>
      <a-form-item label="最大值"><a-input-number v-model="model.max" /></a-form-item>
      <a-form-item v-if="isDecimal" label="小数精度"><a-input-number v-model="model.precision" :min="0" :max="10" /></a-form-item>
      <a-form-item label="步长"><a-input-number v-model="model.step" :min="0" /></a-form-item>
      <a-form-item label="单位"><a-input v-model="model.unit" placeholder="如 ℃、V、kWh" /></a-form-item>
    </a-grid>

    <a-grid v-if="model.type === 'boolean'" :cols="{ xs: 1, md: 2 }" :col-gap="12">
      <a-form-item label="真值"><a-input v-model="model.trueValue" placeholder="true" /></a-form-item>
      <a-form-item label="真值名称"><a-input v-model="model.trueText" placeholder="是" /></a-form-item>
      <a-form-item label="假值"><a-input v-model="model.falseValue" placeholder="false" /></a-form-item>
      <a-form-item label="假值名称"><a-input v-model="model.falseText" placeholder="否" /></a-form-item>
    </a-grid>

    <a-grid v-if="isString" :cols="{ xs: 1, md: 2 }" :col-gap="12">
      <a-form-item label="最大长度"><a-input-number v-model="model.expands.maxLength" :min="1" /></a-form-item>
      <a-form-item label="正则校验"><a-input v-model="model.pattern" placeholder="可选，输入正则表达式" /></a-form-item>
    </a-grid>

    <section v-if="model.type === 'enum'" class="value-type-section">
      <header><strong>枚举项</strong><a-button type="primary" size="small" @click="addEnum"><template #icon><icon-plus /></template>新增枚举</a-button></header>
      <a-empty v-if="!model.elements.length" description="暂无枚举项" />
      <div v-for="(item, index) in model.elements" :key="index" class="enum-row">
        <a-input v-model="item.value" placeholder="枚举值" />
        <a-input v-model="item.text" placeholder="显示名称" />
        <a-input v-model="item.description" placeholder="说明" />
        <a-tooltip content="删除"><a-button type="text" status="danger" shape="circle" @click="model.elements.splice(index, 1)"><icon-delete /></a-button></a-tooltip>
      </div>
    </section>

    <section v-if="model.type === 'object'" class="value-type-section">
      <header><strong>结构字段</strong><a-button type="primary" size="small" @click="addProperty"><template #icon><icon-plus /></template>新增字段</a-button></header>
      <a-empty v-if="!model.properties.length" description="暂无结构字段" />
      <div v-for="(property, index) in model.properties" :key="index" class="property-editor">
        <div class="property-editor__header"><strong>字段 {{ index + 1 }}</strong><a-space><a-switch v-model="property.expands.required" size="small" checked-text="必填" unchecked-text="选填" /><a-tooltip content="删除字段"><a-button type="text" status="danger" shape="circle" @click="model.properties.splice(index, 1)"><icon-delete /></a-button></a-tooltip></a-space></div>
        <a-grid :cols="{ xs: 1, md: 3 }" :col-gap="10">
          <a-form-item label="字段标识"><a-input v-model="property.id" placeholder="字母开头" /></a-form-item>
          <a-form-item label="字段名称"><a-input v-model="property.name" placeholder="请输入名称" /></a-form-item>
          <a-form-item label="字段描述"><a-input v-model="property.description" placeholder="可选" /></a-form-item>
        </a-grid>
        <MetadataValueTypeEditor v-model="property.valueType" :depth="depth + 1" />
      </div>
    </section>

    <section v-if="model.type === 'array'" class="value-type-section">
      <header><strong>数组定义</strong></header>
      <a-grid :cols="{ xs: 1, md: 2 }" :col-gap="12">
        <a-form-item label="最少项数"><a-input-number v-model="model.expands.minItems" :min="0" /></a-form-item>
        <a-form-item label="最多项数"><a-input-number v-model="model.expands.maxItems" :min="1" /></a-form-item>
      </a-grid>
      <div class="array-element"><span>元素类型</span><MetadataValueTypeEditor v-model="model.elementType" :depth="depth + 1" /></div>
    </section>

    <a-grid v-if="model.type === 'file'" :cols="{ xs: 1, md: 3 }" :col-gap="12">
      <a-form-item label="内容类型"><a-select v-model="model.bodyType" :options="bodyTypeOptions" /></a-form-item>
      <a-form-item label="允许格式"><a-input v-model="model.expands.accept" placeholder=".jpg,.png,.zip" /></a-form-item>
      <a-form-item label="大小限制 (MB)"><a-input-number v-model="model.expands.maxSize" :min="1" /></a-form-item>
    </a-grid>

    <a-grid v-if="model.type === 'geoPoint'" :cols="{ xs: 1, md: 2 }" :col-gap="12">
      <a-form-item label="经度字段名"><a-input v-model="model.lonProperty" /></a-form-item>
      <a-form-item label="纬度字段名"><a-input v-model="model.latProperty" /></a-form-item>
    </a-grid>

    <a-form-item v-if="model.type === 'geoShape'" label="默认图形类型"><a-select v-model="model.expands.defaultShapeType" :options="geoShapeOptions" /></a-form-item>
    <a-form-item v-if="model.type === 'date'" label="时间格式"><a-input v-model="model.format" placeholder="timestamp 或 YYYY-MM-DD HH:mm:ss" /></a-form-item>
    <a-alert v-if="model.type === 'password'">密码字段在设备录入和展示时自动脱敏。</a-alert>
  </div>
</template>

<script setup lang="ts">
import type { ValueTypeForm } from './metadataSchema'
import { createValueType } from './metadataSchema'

defineOptions({ name: 'MetadataValueTypeEditor' })

const model = defineModel<ValueTypeForm>({ required: true })
const props = withDefaults(defineProps<{ depth?: number }>(), { depth: 0 })
const depth = computed(() => props.depth)
const isNumber = computed(() => ['int', 'long', 'float', 'double'].includes(model.value.type))
const isDecimal = computed(() => ['float', 'double'].includes(model.value.type))
const isString = computed(() => ['string', 'text'].includes(model.value.type))
const dataTypeOptions = [
  { label: '整数 int', value: 'int' },
  { label: '长整数 long', value: 'long' },
  { label: '浮点数 float', value: 'float' },
  { label: '双精度 double', value: 'double' },
  { label: '布尔 boolean', value: 'boolean' },
  { label: '字符串 string', value: 'string' },
  { label: '文本 text', value: 'text' },
  { label: '日期 date', value: 'date' },
  { label: '枚举 enum', value: 'enum' },
  { label: '结构体 object', value: 'object' },
  { label: '数组 array', value: 'array' },
  { label: '文件 file', value: 'file' },
  { label: '密码 password', value: 'password' },
  { label: '地理位置 geoPoint', value: 'geoPoint' },
  { label: '地理形状 geoShape', value: 'geoShape' }
]
const bodyTypeOptions = [{ label: 'URL', value: 'url' }, { label: 'Base64', value: 'base64' }, { label: 'Binary', value: 'binary' }]
const geoShapeOptions = ['Point', 'LineString', 'Polygon', 'MultiPoint', 'MultiLineString', 'MultiPolygon', 'GeometryCollection'].map((value) => ({ label: value, value }))

function changeType(value: unknown) {
  model.value = createValueType(String(value || 'string'))
}

function addEnum() {
  model.value.elements.push({ value: '', text: '', description: '' })
}

function addProperty() {
  model.value.properties.push({ id: '', name: '', description: '', expands: {}, valueType: createValueType('string') })
}
</script>

<style lang="scss" scoped>
.value-type-editor {
  padding: 12px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;

  &--nested {
    background: var(--color-fill-1);
  }
}

.value-type-section {
  margin-top: 8px;

  > header,
  .property-editor__header {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }
}

.enum-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr 32px;
  gap: 8px;
  margin-top: 8px;
}

.property-editor {
  padding: 12px;
  margin-top: 10px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
}

.array-element > span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-2);
}

@media (width <= 680px) {
  .enum-row {
    grid-template-columns: 1fr;
  }
}
</style>
