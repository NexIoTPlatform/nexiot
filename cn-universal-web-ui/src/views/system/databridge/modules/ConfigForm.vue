<template>
  <a-modal
    :title="formData.id ? '编辑桥接规则管理' : '新增桥接规则管理'"
    :visible="visible"
    :confirm-loading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
    width="60vw"
    :style="{ maxWidth: '700px', minWidth: '450px' }"
    :body-style="{ padding: '0', maxHeight: '70vh', overflow: 'auto' }"
    :footer="null"
    :keyboard="!magicEditorFullscreen"
    :mask-closable="!magicEditorFullscreen"
    class="config-form-modal"
  >
    <!-- 步骤指示器 -->
    <div class="step-indicator">
      <div class="step-item" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <div class="step-number">1</div>
        <div class="step-title">基本信息</div>
      </div>
      <div class="step-line" :class="{ active: currentStep > 1 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <div class="step-number">2</div>
        <div class="step-title">数据源配置</div>
      </div>
      <div class="step-line" :class="{ active: currentStep > 2 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 3 }">
        <div class="step-number">3</div>
        <div class="step-title">处理配置</div>
      </div>
    </div>

    <div class="form-container">
      <a-form-model
        ref="form"
        :model="form"
        :rules="rules"
        class="config-form"
      >
        <!-- 步骤1: 基本信息 -->
        <div v-show="currentStep === 1" class="step-content">
          <div class="step-header">
            <h3>基本信息配置</h3>
            <p>请填写桥接规则的基本信息</p>
          </div>

          <div class="form-section">
            <a-form-model-item label="桥接规则名称" prop="name" class="form-item-large">
              <a-input
                v-model="form.name"
                placeholder="请输入桥接规则名称"
                size="large"
              />
              <div class="form-help-text">建议使用有意义的名称，便于后续管理</div>
            </a-form-model-item>

            <a-form-model-item label="数据源范围" prop="sourceScope" class="form-item-large">
              <div class="scope-cards">
                <div
                  class="scope-card"
                  :class="{ active: form.sourceScope === 'ALL_PRODUCTS', disabled: isEditAndEnabled }"
                  @click="!isEditAndEnabled && selectSourceScope('ALL_PRODUCTS')"
                >
                  <div class="card-icon">
                    <a-icon type="global"/>
                  </div>
                  <div class="card-content">
                    <h4>所有产品</h4>
                    <p>ALL_PRODUCTS</p>
                    <div class="card-desc">适用于所有产品的数据桥接</div>
                  </div>
                  <div class="card-check">
                    <a-icon type="check" v-if="form.sourceScope === 'ALL_PRODUCTS'"/>
                  </div>
                </div>
                <div
                  class="scope-card"
                  :class="{ active: form.sourceScope === 'SPECIFIC_PRODUCTS', disabled: isEditAndEnabled }"
                  @click="!isEditAndEnabled && selectSourceScope('SPECIFIC_PRODUCTS')"
                >
                  <div class="card-icon">
                    <a-icon type="appstore"/>
                  </div>
                  <div class="card-content">
                    <h4>指定产品</h4>
                    <p>SPECIFIC_PRODUCTS</p>
                    <div class="card-desc">针对指定产品的数据桥接</div>
                  </div>
                  <div class="card-check">
                    <a-icon type="check" v-if="form.sourceScope === 'SPECIFIC_PRODUCTS'"/>
                  </div>
                </div>
                <div
                  class="scope-card"
                  :class="{ active: form.sourceScope === 'APPLICATION', disabled: isEditAndEnabled }"
                  @click="!isEditAndEnabled && selectSourceScope('APPLICATION')"
                >
                  <div class="card-icon">
                    <a-icon type="api"/>
                  </div>
                  <div class="card-content">
                    <h4>应用级别</h4>
                    <p>APPLICATION</p>
                    <div class="card-desc">针对特定应用的数据桥接</div>
                  </div>
                  <div class="card-check">
                    <a-icon type="check" v-if="form.sourceScope === 'APPLICATION'"/>
                  </div>
                </div>
              </div>
            </a-form-model-item>

            <!-- 指定产品：多选产品 -->
            <a-form-model-item v-if="form.sourceScope === 'SPECIFIC_PRODUCTS'" label="选择产品" prop="sourceProductKeys"
                               class="form-item-large">
              <a-select
                v-model="form.sourceProductKeys"
                mode="multiple"
                placeholder="请选择产品（可多选）"
                style="width: 100%"
                size="large"
                allow-clear>
                <a-select-option v-for="(p) in productList" :key="p.productKey" :value="p.productKey">
                  {{ p.name }} ({{ p.productKey }})
                </a-select-option>
              </a-select>
              <div class="form-help-text">选择需要桥接数据的特定产品</div>
            </a-form-model-item>

            <!-- 应用：单选应用 -->
            <a-form-model-item v-if="form.sourceScope === 'APPLICATION'" label="选择应用" prop="sourceApplicationId"
                               class="form-item-large">
              <a-select
                v-model="form.sourceApplicationId"
                placeholder="请选择应用"
                style="width: 100%"
                size="large"
                allow-clear>
                <a-select-option v-for="(d, index) in applicationList" :key="index" :value="d.appUniqueId">
                  {{ d.appName }}
                </a-select-option>
              </a-select>
              <div class="form-help-text">选择需要桥接数据的特定应用</div>
            </a-form-model-item>

            <a-form-model-item :label="$t('common.status')" prop="status" class="form-item-large">
              <a-radio-group v-model="form.status" class="status-radio-group">
                <a-radio :value="1" class="status-radio">
                  <a-icon type="check-circle" style="color: #52c41a; margin-right: 8px;"/>
                  {{ $t('status.enable') }}
                </a-radio>
                <a-radio :value="0" class="status-radio">
                  <a-icon type="close-circle" style="color: #ff4d4f; margin-right: 8px;"/>
                  {{ $t('status.disable') }}
                </a-radio>
              </a-radio-group>
            </a-form-model-item>

            <a-form-model-item label="描述" prop="description" class="form-item-large">
              <a-textarea
                v-model="form.description"
                placeholder="请输入桥接规则描述"
                :rows="3"
                class="description-textarea"
              />
            </a-form-model-item>
          </div>
        </div>

        <!-- 步骤2: 数据源配置 -->
        <div v-show="currentStep === 2" class="step-content">
          <div class="step-header">
            <h3>数据源配置</h3>
            <p>配置目标资源和桥接类型</p>
          </div>

          <div class="form-section">
            <a-form-model-item label="目标资源" prop="targetResourceId" class="form-item-large">
              <a-select
                v-model="form.targetResourceId"
                placeholder="请选择目标资源"
                @change="handleResourceChange"
                size="large"
                style="width: 100%"
              >
                <a-select-option
                  v-for="resource in filteredResourceList"
                  :key="resource.id"
                  :value="resource.id"
                >
                  <div class="resource-option">
                    <span class="resource-name">{{ resource.name }}</span>
                    <a-tag :color="getResourceTypeColor(resource.type)" size="small">
                      {{ getResourceTypeName(resource.type) }}
                    </a-tag>
                    <a-tag v-if="resource.pluginType" :color="getPluginTypeColor(resource.pluginType)" size="small">
                      {{ resource.pluginType }}
                    </a-tag>
                    <a-tag :color="getDirectionColor(resource.direction)" size="small">
                      {{ getDirectionText(resource.direction) }}
                    </a-tag>
                  </div>
                </a-select-option>
              </a-select>
              <div class="form-help-text">选择数据要发送到的目标资源</div>
            </a-form-model-item>

            <a-form-model-item label="桥接类型" prop="bridgeType" class="form-item-large">
              <a-select
                v-model="form.bridgeType"
                placeholder="请选择桥接类型"
                @change="handleTypeChange"
                :disabled="isEditAndEnabled"
                size="large"
                style="width: 100%"
              >
                <a-select-option value="JDBC">
                  <a-icon type="database" style="margin-right: 8px;"/>
                  JDBC数据库
                </a-select-option>
                <a-select-option value="KAFKA">
                  <a-icon type="message" style="margin-right: 8px;"/>
                  Kafka消息队列
                </a-select-option>
                <a-select-option value="MQTT">
                  <a-icon type="wifi" style="margin-right: 8px;"/>
                  MQTT消息代理
                </a-select-option>
                <a-select-option value="HTTP">
                  <a-icon type="api" style="margin-right: 8px;"/>
                  HTTP接口
                </a-select-option>
                <a-select-option value="IOTDB">
                  <a-icon type="line-chart" style="margin-right: 8px;"/>
                  IoTDB时序数据库
                </a-select-option>
                <a-select-option value="INFLUXDB">
                  <a-icon type="line-chart" style="margin-right: 8px;"/>
                  InfluxDB时序数据库
                </a-select-option>
              </a-select>
              <div class="form-help-text">选择数据桥接的处理类型</div>
            </a-form-model-item>

            <a-form-model-item label="模板内容" prop="template" class="form-item-large">
              <a-textarea
                v-model="form.template"
                placeholder="请输入模板内容"
                :rows="6"
                class="template-textarea"
              />
              <div class="form-help-text">
                <p>模板变量：</p>
                <ul>
                  <li><code>{deviceId}</code> - 设备ID</li>
                  <li><code>{productKey}</code> - 产品KEY</li>
                  <li><code>{messageType}</code> - 消息类型</li>
                  <li><code>{timestamp}</code> - 时间戳</li>
                  <li><code>{properties}</code> - 属性JSON</li>
                </ul>
                <div v-if="form.bridgeType" class="template-example">
                  <p>当前类型示例：</p>
                  <pre class="code-block">{{ getTemplateExample() }}</pre>
                </div>
              </div>
            </a-form-model-item>
          </div>
        </div>

        <!-- 步骤3: 处理配置 -->
        <div v-show="currentStep === 3" class="step-content">
          <div class="step-header">
            <h3>处理配置</h3>
            <p>配置数据处理逻辑和高级选项</p>
          </div>

          <div class="form-section">
            <a-form-model-item label="Magic脚本" prop="magicScript" class="form-item-large">
              <div class="editor-toolbar">
                <a-button size="small" icon="fullscreen" @click="toggleMagicEditorFullscreen(true)">全屏编辑</a-button>
              </div>
              <div class="editor-adaptive">
                <CodeEditor
                  v-model="form.magicScript"
                  :options="{mode: 'javascript', theme: 'material'}"
                  style="width:100%;height:100%;"
                />
              </div>
              <div class="form-help-text">
                <p>Magic脚本用于自定义数据处理逻辑，可选配置：</p>
                <ul>
                  <li><code>function iotToYour(data, config)</code> - 处理IoT平台到外部系统的数据，payload 为 request 对象</li>
                  <li><code>function yourToIot(data,config)</code> - 处理外部系统到IoT平台的数据，payload 为 externalData 对象</li>
                </ul>
                <div class="script-example">
                  <p>示例脚本（推荐写法，已自动转换不兼容语法）：</p>
                  <pre class="code-block">// iotToYour: IoT平台到外部系统
function iotToYour(data,config){
  return {
    deviceId: data.deviceId,
    timestamp: data.timestamp,
    data: data.properties
  };
}

// yourToIot: 外部系统到IoT平台
function yourToIot(data,config){
  return {
    deviceKey: data.deviceId,
    properties: data.data
  };
}</pre>
                </div>
              </div>
            </a-form-model-item>

            <a-form-model-item label="统一配置" prop="config" class="form-item-large">
              <a-textarea
                v-model="form.config"
                placeholder="请输入JSON格式的统一配置（如字段映射、过滤条件、批量、重试、headers等）"
                :rows="6"
                class="config-textarea"
              />
              <div class="form-help-text">
                <p>统一配置示例：</p>
                <pre class="code-block">{
  "field_mapping": {"deviceKey": "device_id"},
  "filter_conditions": {"messageType": ["PROPERTY_POST"]},
  "batch_size": 100,
  "retry_count": 3,
  "headers": {"Authorization": "Bearer token"}
}</pre>
              </div>
            </a-form-model-item>
          </div>
        </div>
      </a-form-model>
    </div>

    <!-- 底部按钮 -->
    <div class="modal-footer">
      <div class="footer-left">
        <a-button @click="handleCancel" size="large">{{ $t('button.cancel') }}
        </a-button>
      </div>
      <div class="footer-right">
        <a-button
          v-if="currentStep > 1"
          @click="prevStep"
          size="large"
          style="margin-right: 12px;"
        >
          上一步
        </a-button>
        <a-button
          v-if="currentStep < 3"
          type="primary"
          @click="nextStep"
          size="large"
        >
          下一步
        </a-button>
        <a-button
          v-if="currentStep === 3"
          type="primary"
          @click="handleOk"
          :loading="confirmLoading"
          size="large"
        >
          完成
        </a-button>
      </div>
    </div>

    <!-- 全屏编辑覆盖层 -->
    <div v-if="magicEditorFullscreen" class="fullscreen-overlay">
      <div class="fullscreen-toolbar">
        <div class="toolbar-left">
          <span>Magic脚本 全屏编辑</span>
        </div>
        <div class="toolbar-right">
          <a-button icon="fullscreen-exit" @click="toggleMagicEditorFullscreen(false)">退出全屏</a-button>
        </div>
      </div>
      <div class="fullscreen-editor">
        <CodeEditor
          v-model="form.magicScript"
          :options="{mode: 'javascript', theme: 'material'}"
          style="width:100%;height:100%;"
        />
      </div>
    </div>
  </a-modal>
</template>

<script>
import { listApplication } from '@/api/application/application';
import { listProduct } from '@/api/system/dev/product';
import CodeEditor from '@/components/CodeEditor.vue';
import DataBridgeMappings from '@/utils/databridge-mappings';

export default {
  name: 'ConfigForm',
  components: {
    CodeEditor
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    formData: {
      type: Object,
      default: () => ({})
    },
    resourceList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      confirmLoading: false,
      currentStep: 1, // 当前步骤
      magicEditorFullscreen: false,
      form: {
        name: '',
        sourceScope: 'ALL_PRODUCTS',
        sourceProductKeys: [],
        sourceApplicationId: undefined,
        targetResourceId: null,
        bridgeType: '',
        template: '',
        magicScript: '',
        config: '',
        status: 1,
        description: ''
      },
      selectedResource: null,
      productList: [],
      applicationList: [],
      rules: {
        name: [
          {required: true, message: '请输入桥接规则名称', trigger: 'blur'}
        ],
        sourceScope: [
          {required: true, message: '请选择数据源范围', trigger: 'change'}
        ],
        targetResourceId: [
          {required: true, message: '请选择目标资源', trigger: 'change'}
        ],
        bridgeType: [
          {required: true, message: '请选择桥接类型', trigger: 'change'}
        ],
        template: [
          {required: true, message: '请输入模板内容', trigger: 'blur'}
        ]
      }
    }
  },
  computed: {
    // 过滤资源列表，根据当前桥接方向
    filteredResourceList() {
      if (!this.resourceList) return []
      return this.resourceList
    },
    // 判断是否为编辑模式且已启用
    isEditAndEnabled() {
      return this.formData && this.formData.id && this.form.status === 1
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initForm()
        this.loadData()
      } else {
        // 关闭弹窗时，确保退出全屏状态
        this.magicEditorFullscreen = false
      }
    },
    formData: {
      handler() {
        this.initForm()
      },
      deep: true
    },
    // 当进入步骤3（处理配置）时，触发一次编辑器刷新，避免首次显示不渲染
    currentStep(newVal) {
      if (newVal === 3) {
        this.$nextTick(() => {
          window.dispatchEvent(new Event('resize'))
        })
      }
    }
  },
  methods: {
    // 简单的 HTML 实体反解码，防止脚本中的符号被转义
    decodeHtmlEntities(text) {
      if (!text || typeof text !== 'string') return text
      return text
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
    },
    // 全屏下的键盘拦截（ESC 退出全屏而不是关闭弹窗）
    onFullscreenKeydown(e) {
      if (e && (e.key === 'Escape' || e.keyCode === 27)) {
        e.preventDefault()
        e.stopPropagation()
        this.toggleMagicEditorFullscreen(false)
      }
    },
    // 切换 Magic 脚本编辑器全屏
    toggleMagicEditorFullscreen(isFullscreen) {
      this.magicEditorFullscreen = !!isFullscreen
      // 进入全屏时，监听 ESC；退出时，移除监听
      if (this.magicEditorFullscreen) {
        window.addEventListener('keydown', this.onFullscreenKeydown, true)
      } else {
        window.removeEventListener('keydown', this.onFullscreenKeydown, true)
      }
      this.$nextTick(() => {
        // 触发编辑器尺寸更新
        window.dispatchEvent(new Event('resize'))
      })
    },
    initForm() {
      if (this.formData && Object.keys(this.formData).length > 0) {
        this.form = {...this.formData}
        // 反解码后端返回可能转义的脚本内容
        if (this.form.magicScript) {
          this.form.magicScript = this.decodeHtmlEntities(this.form.magicScript)
        }
        // 处理数组字段
        if (this.form.sourceProductKeys && typeof this.form.sourceProductKeys === 'string') {
          try {
            this.form.sourceProductKeys = JSON.parse(this.form.sourceProductKeys)
          } catch (e) {
            this.form.sourceProductKeys = []
          }
        }
      } else {
        this.form = {
          name: '',
          sourceScope: 'ALL_PRODUCTS',
          sourceProductKeys: [],
          sourceApplicationId: undefined,
          targetResourceId: null,
          bridgeType: '',
          template: '',
          magicScript: '',
          config: '',
          status: 1,
          description: ''
        }
      }
      this.currentStep = 1
    },

    // 步骤控制方法
    nextStep() {
      if (this.currentStep === 1) {
        // 验证第一步
        if (!this.form.name) {
          this.$message.error('请输入桥接规则名称')
          return
        }
        if (!this.form.sourceScope) {
          this.$message.error('请选择数据源范围')
          return
        }
        if (this.form.sourceScope === 'SPECIFIC_PRODUCTS' && (!this.form.sourceProductKeys || this.form.sourceProductKeys.length === 0)) {
          this.$message.error('请选择至少一个产品')
          return
        }
        if (this.form.sourceScope === 'APPLICATION' && !this.form.sourceApplicationId) {
          this.$message.error('请选择应用')
          return
        }
        this.currentStep = 2
      } else if (this.currentStep === 2) {
        // 验证第二步
        if (!this.form.targetResourceId) {
          this.$message.error('请选择目标资源')
          return
        }
        if (!this.form.bridgeType) {
          this.$message.error('请选择桥接类型')
          return
        }
        if (!this.form.template) {
          this.$message.error('请输入模板内容')
          return
        }
        this.currentStep = 3
        // 进入步骤3后刷新编辑器，避免首次显示不渲染
        this.$nextTick(() => {
          window.dispatchEvent(new Event('resize'))
        })
      }
    },

    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },

    // 选择数据源范围
    selectSourceScope(scope) {
      this.form.sourceScope = scope
      // 清空相关字段
      if (scope !== 'SPECIFIC_PRODUCTS') {
        this.form.sourceProductKeys = []
      }
      if (scope !== 'APPLICATION') {
        this.form.sourceApplicationId = undefined
      }
    },

    // 加载产品列表和应用列表
    loadData() {
      this.loadProductList()
      this.loadApplicationList()
    },

    // 加载产品列表
    loadProductList() {
      listProduct({
        state: 0,
        pageNum: 1,
        pageSize: 99999999
      }).then(res => {
        this.productList = res.rows || []
      }).catch(err => {
        console.error('加载产品列表失败:', err)
        this.$message.error('加载产品列表失败')
      })
    },

    // 加载应用列表
    loadApplicationList() {
      listApplication({
        pageNum: 1,
        pageSize: 99999999
      }).then(res => {
        this.applicationList = res.rows || []
      }).catch(err => {
        console.error('加载应用列表失败:', err)
        this.$message.error('加载应用列表失败')
      })
    },

    handleResourceChange(value) {
      // 根据选择的资源自动设置桥接类型
      const resource = this.resourceList.find(r => r.id === value)
      if (resource) {
        this.selectedResource = resource
        // 优先使用资源连接的插件类型，如果没有则使用资源类型
        this.form.bridgeType = resource.pluginType || resource.type
      }
    },

    handleTypeChange(value) {
      // 根据桥接类型设置默认模板
      if (!this.form.template) {
        this.form.template = this.getTemplateExample()
      }
    },

    // 获取模板示例
    getTemplateExample() {
      const examples = {
        'JDBC': 'INSERT INTO device_data (device_key, message_type, timestamp, properties) VALUES (\'{deviceKey}\', \'{messageType}\', \'{timestamp}\', \'{properties}\')',
        'KAFKA': '{\n  "deviceKey": "{deviceKey}",\n  "messageType": "{messageType}",\n  "timestamp": "{timestamp}",\n  "properties": {properties}\n}',
        'MQTT': '{properties}',
        'HTTP': '{\n  "deviceKey": "{deviceKey}",\n  "data": {properties}\n}',
        'IOTDB': 'INSERT INTO root.device_data (device_key, message_type, timestamp, properties) VALUES (\'{deviceKey}\', \'{messageType}\', {timestamp}, \'{properties}\')',
        'INFLUXDB': 'device_data,device_key={deviceKey},message_type={messageType} properties="{properties}" {timestamp}'
      }
      return examples[this.form.bridgeType] || ''
    },

    // 使用统一映射文件的方法
    getResourceTypeName(type) {
      return DataBridgeMappings.getResourceType(type).name
    },

    getResourceTypeColor(type) {
      return DataBridgeMappings.getResourceType(type).color
    },

    getPluginTypeColor(pluginType) {
      return DataBridgeMappings.getPluginType(pluginType).color
    },

    getDirectionColor(direction) {
      const colorMap = {
        'OUT': 'success',
        'IN': 'processing',
        'BOTH': 'warning'
      }
      return colorMap[direction] || 'default'
    },

    getDirectionText(direction) {
      const textMap = {
        'OUT': '输出',
        'IN': '输入',
        'BOTH': '双向'
      }
      return textMap[direction] || '未知'
    },

    handleOk() {
      // 如果是分步模式，确保在最后一步
      if (this.currentStep < 3) {
        this.nextStep()
        return
      }

      this.$refs.form.validate(valid => {
        if (valid) {
          this.confirmLoading = true

          // 验证JSON格式字段
          const jsonFields = ['config']
          for (const field of jsonFields) {
            if (this.form[field]) {
              try {
                JSON.parse(this.form[field])
              } catch (error) {
                this.$message.error(`统一配置 JSON格式错误`)
                this.confirmLoading = false
                return
              }
            }
          }

          // 处理数组字段
          const formData = {...this.form}
          // 确保提交前脚本未被 HTML 转义
          if (formData.magicScript) {
            formData.magicScript = this.decodeHtmlEntities(formData.magicScript)
          }
          if (formData.sourceProductKeys && Array.isArray(formData.sourceProductKeys)) {
            formData.sourceProductKeys = JSON.stringify(formData.sourceProductKeys)
          }

          this.$emit('ok', formData)
          this.confirmLoading = false
        }
      })
    },

    handleCancel() {
      // 关闭弹窗时重置全屏状态和监听
      if (this.magicEditorFullscreen) {
        this.magicEditorFullscreen = false
        window.removeEventListener('keydown', this.onFullscreenKeydown, true)
      }
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
/* 步骤指示器样式 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #d9d9d9;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.step-item.active .step-number {
  background: #1890ff;
}

.step-item.completed .step-number {
  background: #52c41a;
}

.step-title {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.step-item.active .step-title {
  color: #1890ff;
}

.step-line {
  width: 120px;
  height: 2px;
  background: #d9d9d9;
  margin: 0 16px;
  margin-top: -20px;
  transition: all 0.3s ease;
}

.step-line.active {
  background: #1890ff;
}

/* 表单容器样式 */
.form-container {
  padding: 16px;
  min-height: 350px;
}

.step-content {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-header {
  margin-bottom: 16px;
  text-align: center;
}

.step-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.step-header p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

/* 表单区域样式 */
.form-section {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 20px;
}

.form-item-large {
  margin-bottom: 16px;
}

.form-help-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  line-height: 1.4;
}

.form-help-text ul {
  margin: 4px 0;
  padding-left: 16px;
}

.form-help-text li {
  margin: 4px 0;
}

.form-help-text code {
  background: #f0f0f0;
  padding: 2px 4px;
  border-radius: 2px;
  font-size: 11px;
}

/* 数据源范围卡片样式 */
.scope-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 6px;
}

.scope-card {
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  background: #fff;
}

.scope-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.scope-card.active {
  border-color: #1890ff;
  background: #f6ffed;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.scope-card.disabled {
  cursor: not-allowed;
  opacity: 0.6;
  background: #f5f5f5;
}

.scope-card.disabled:hover {
  border-color: #e8e8e8;
  box-shadow: none;
}

.card-icon {
  font-size: 20px;
  color: #1890ff;
  margin-bottom: 8px;
}

.card-content h4 {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 3px 0;
}

.card-content p {
  font-size: 12px;
  color: #1890ff;
  font-weight: 500;
  margin: 0 0 6px 0;
}

.card-desc {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.3;
}

.card-check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 20px;
  height: 20px;
  background: #52c41a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}

/* 资源选项样式 */
.resource-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-name {
  flex: 1;
}

/* 状态单选组样式 */
.status-radio-group {
  display: flex;
  gap: 24px;
}

.status-radio {
  display: flex;
  align-items: center;
  font-size: 14px;
}

/* 代码块样式 */
.code-block {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #262626;
  overflow-x: auto;
  margin: 8px 0;
  line-height: 1.4;
}

/* 编辑器适配样式 */
.editor-adaptive {
  width: 100%;
  height: 300px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}

/* 编辑器工具栏 */
.editor-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 8px;
}

/* 文本域样式 */
.template-textarea,
.config-textarea {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.description-textarea {
  resize: vertical;
  min-height: 80px;
}

/* 模板示例样式 */
.template-example {
  margin-top: 12px;
}

.script-example {
  margin-top: 12px;
}

/* 底部按钮样式 */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
}

.footer-left {
  flex: 1;
}

.footer-right {
  display: flex;
  align-items: center;
}


@media (max-width: 1200px) {
  .scope-cards {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 10px;
  }

  .form-container {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .scope-cards {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .step-indicator {
    padding: 12px 16px;
  }

  .step-line {
    width: 40px;
  }

  .form-container {
    padding: 16px;
  }

  .modal-footer {
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
  }

  .footer-right {
    width: 100%;
    justify-content: center;
  }

  .scope-card {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .step-indicator {
    padding: 8px 12px;
  }

  .step-line {
    width: 20px;
  }

  .form-container {
    padding: 12px;
  }

  .scope-card {
    padding: 10px;
  }
}

/* 表单验证样式增强 */
.ant-form-item-has-error .scope-card {
  border-color: #ff4d4f;
}

.ant-form-item-has-error .scope-card.active {
  border-color: #ff4d4f;
  background: #fff2f0;
}

/* 全屏覆盖层样式 */
.fullscreen-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #0f0f0f;
  z-index: 10000; /* 高于模态框 */
}

.fullscreen-toolbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #1f1f1f;
  color: #fff;
  z-index: 10001;
  border-bottom: 1px solid rgba(255,255,255,0.12);
}

.fullscreen-editor {
  position: fixed;
  top: 48px;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10000;
}
</style>