<template>
  <a-collapse class="ota-guide" :default-active-key="['steps']" :bordered="false">
    <a-collapse-item key="steps" header="第一次用？升级只需 3 步">
      <ol class="ota-guide__steps">
        <li><strong>上传完整固件</strong><span>选产品、填版本，上传后发布升级包。分包也上传完整文件。</span></li>
        <li><strong>选择目标设备</strong><span>创建升级计划，建议先选 1 台验证，再扩大范围。</span></li>
        <li><strong>开始并查看结果</strong><span>平台按厂家适配整包或分包传输，在设备明细查看进度和失败原因。</span></li>
      </ol>
      <div class="ota-guide__note">首次接入厂家，由技术人员配置一次“厂家适配”，后续升级直接复用。</div>
    </a-collapse-item>
  </a-collapse>
</template>

<script setup lang="ts">
defineOptions({ name: 'IotOtaQuickGuide' })
</script>

<style lang="scss" scoped>
.ota-guide {
  flex-shrink: 0;
  margin-bottom: 16px;
  background: var(--color-fill-1);
  border-radius: 6px;

  &__steps {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 28px;
    padding: 0 0 0 22px;
    margin: 0;

    li::marker {
      font-weight: 600;
      color: rgb(var(--primary-6));
    }

    strong,
    span {
      display: block;
    }

    span {
      margin-top: 5px;
      font-size: 12px;
      line-height: 1.7;
      color: var(--color-text-3);
    }
  }

  &__note {
    margin-top: 12px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

@media (width <= 768px) {
  .ota-guide__steps {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
