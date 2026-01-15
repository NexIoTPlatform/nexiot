<template>
  <a-modal
    :visible="open"
    :title="formTitle"
    width="650px"
    centered
    :footer="null"
    @cancel="onClose"
    :maskClosable="false"
    destroyOnClose
  >
    <a-spin :spinning="submitLoading" tip="正在保存，请稍候..." size="large">
      <a-form-model ref="form" :model="form" :rules="rules" layout="vertical">
        <a-form-model-item label="网络类型" prop="type" style="margin-bottom: 12px;">
          <a-select
            v-model="form.type"
            style="width: 100%"
            placeholder="请选择网络类型"
            :disabled="submitLoading"
            @change="handleTypeChange"
          >
            <a-select-option
              v-for="item in networkTypeOptions"
              :key="item.dictValue"
              :value="item.dictValue"
            >
              <span class="type-cell">
                <a-icon
                  :type="item.dictValue === 'TCP_CLIENT' ? 'user' :
                    item.dictValue === 'TCP_SERVER' ? 'cloud-server' :
                    item.dictValue === 'MQTT_CLIENT' ? 'cloud' :
                    item.dictValue === 'UDP' ? 'wifi' : 'cloud-server'"
                  :style="{ color: item.dictValue === 'TCP_CLIENT' ? '#1890ff' :
                              item.dictValue === 'TCP_SERVER' ? '#52c41a' :
                              item.dictValue === 'MQTT_CLIENT' ? '#fa8c16' :
                              item.dictValue === 'UDP' ? '#722ed1' : '#eb2f96',
                            marginRight: '8px' }"
                />
                <span>{{ item.dictLabel }}</span>
              </span>
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item v-if="isTCP" :label="$t('compound.productProductKey')" prop="productKey"
                           style="margin-bottom: 12px;">
          <a-select
            v-model="productKeyProxy"
            placeholder="请选择或搜索产品名称"
            show-search
            :filter-option="false"
            :loading="productSearchLoading"
            @search="handleProductNameSearch"
            @focus="handleProductNameFocus"
            allow-clear
            style="width: 100%"
            @change="handleProductNameChange"
          >
            <a-select-option
              v-for="product in productSearchList"
              :key="product.productKey"
              :value="String(product.productKey)"
              :title="product.name"
              :disabled="usedProductKeys.includes(product.productKey)"
            >
              <div class="product-search-option">
                <div class="product-search-name">{{ product.name }}</div>
                <div class="product-search-key">
                  {{ product.productKey }}
                  <span v-if="usedProductKeys.includes(product.productKey)"
                        style="color:#f5222d;font-size:12px;margin-left:8px;">已占用</span>
                </div>
              </div>
            </a-select-option>
          </a-select>
          <div v-if="productKeyProxy" class="product-key-selected ellipsis">
            产品名称: {{ getSelectedProductName() }}<br>
            ProductKey: {{ productKeyProxy }}
          </div>
        </a-form-model-item>
        <a-form-model-item v-if="isWebSocket" :label="$t('compound.productProductKey')" prop="productKey"
                           style="margin-bottom: 12px;">
          <a-select
            v-model="productKeyProxy"
            placeholder="请选择或搜索产品名称"
            show-search
            :filter-option="false"
            :loading="productSearchLoading"
            @search="handleProductNameSearch"
            @focus="handleProductNameFocus"
            allow-clear
            style="width: 100%"
            @change="handleProductNameChange"
          >
            <a-select-option
              v-for="product in productSearchList"
              :key="product.productKey"
              :value="String(product.productKey)"
              :title="product.name"
              :disabled="usedProductKeys.includes(product.productKey)"
            >
              <div class="product-search-option">
                <div class="product-search-name">{{ product.name }}</div>
                <div class="product-search-key">
                  {{ product.productKey }}
                  <span v-if="usedProductKeys.includes(product.productKey)"
                        style="color:#f5222d;font-size:12px;margin-left:8px;">已占用</span>
                </div>
              </div>
            </a-select-option>
          </a-select>
          <div v-if="productKeyProxy" class="product-key-selected ellipsis">
            产品名称: {{ getSelectedProductName() }}<br>
            ProductKey: {{ productKeyProxy }}
          </div>
        </a-form-model-item>
        <!-- WebSocket 配置字段 -->
        <template v-if="isWebSocket">
          <a-row :gutter="16" v-if="isWebSocketClient">
            <a-col :span="24">
              <a-form-model-item label="服务器地址" required style="margin-bottom: 12px;">
                <a-input
                  v-model="wsConfig.host"
                  style="width:100%"
                  placeholder="如: 192.168.1.100 或 ws://example.com"
                  :disabled="submitLoading"
                >
                  <a-icon slot="prefix" type="global"/>
                </a-input>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-model-item :label="isWebSocketClient ? '服务器端口' : '监听端口'" required style="margin-bottom: 12px;">
                <a-input-number
                  v-model="wsConfig.port"
                  style="width:100%"
                  placeholder="请输入端口号"
                  :min="1"
                  :max="65535"
                  :disabled="submitLoading"
                />
              </a-form-model-item>
            </a-col>
            <a-col :span="12">
              <a-form-model-item label="路径" required style="margin-bottom: 12px;">
                <a-input
                  v-model="wsConfig.path"
                  style="width:100%"
                  placeholder="如: /ws"
                  :disabled="submitLoading"
                >
                  <a-icon slot="prefix" type="link"/>
                </a-input>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-model-item label="ClientId" style="margin-bottom: 12px;">
                <a-input
                  v-model="wsConfig.clientId"
                  style="width:100%"
                  placeholder="不填则自动生成UUID"
                  :disabled="submitLoading"
                >
                  <a-icon slot="prefix" type="idcard"/>
                </a-input>
              </a-form-model-item>
            </a-col>
            <a-col :span="12">
              <a-form-model-item label="最大连接数" style="margin-bottom: 12px;">
                <a-input-number
                  v-model="wsConfig.maxConnections"
                  style="width:100%"
                  placeholder="默认1000"
                  :min="1"
                  :disabled="submitLoading"
                />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-model-item label="用户名" style="margin-bottom: 12px;">
                <a-input
                  v-model="wsConfig.username"
                  style="width:100%"
                  placeholder="可选"
                  :disabled="submitLoading"
                >
                  <a-icon slot="prefix" type="user"/>
                </a-input>
              </a-form-model-item>
            </a-col>
            <a-col :span="12">
              <a-form-model-item label="密码" style="margin-bottom: 12px;">
                <a-input-password
                  v-model="wsConfig.password"
                  style="width:100%"
                  placeholder="可选"
                  :disabled="submitLoading"
                >
                  <a-icon slot="prefix" type="lock"/>
                </a-input-password>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-form-model-item v-if="isWebSocketClient" label="子协议" style="margin-bottom: 12px;">
            <a-input
              v-model="wsConfig.subProtocol"
              style="width:100%"
              placeholder="可选，如: mqtt, stomp, wamp等"
              :disabled="submitLoading"
            >
              <a-icon slot="prefix" type="api"/>
            </a-input>
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              WebSocket子协议（Sec-WebSocket-Protocol），MQTT服务需填写"mqtt"
            </div>
          </a-form-model-item>
          <a-form-model-item v-if="isWebSocketClient" label="订阅主题" style="margin-bottom: 12px;">
            <a-input
              v-model="wsConfig.topics"
              style="width:100%"
              placeholder="多个主题用逗号分隔，如: sensor/temperature,sensor/humidity,device/status"
              :disabled="submitLoading"
            >
              <a-icon slot="prefix" type="tags"/>
            </a-input>
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              可选字段，适用于 MQTT over WebSocket，支持通配符，多个主题用逗号分隔
            </div>
          </a-form-model-item>
          <a-form-model-item label="允许跨域" style="margin-bottom: 12px;">
            <a-switch v-model="wsConfig.allowOrigins" :disabled="submitLoading" />
            <span style="margin-left: 8px; color: #999;">允许所有来源的跨域请求</span>
          </a-form-model-item>
        </template>
        <a-form-model-item label="组件名称" prop="name" style="margin-bottom: 12px;">
          <a-input
            v-model="form.name"
            style="width:100%"
            placeholder="请输入网络组件名称"
            :disabled="submitLoading"
          >
            <a-icon slot="prefix" type="tag"/>
          </a-input>
        </a-form-model-item>
        <a-form-model-item label="启用状态" prop="state" style="margin-bottom: 12px;">
          <a-select
            v-model="form.state"
            style="width:100%"
            placeholder="请选择启用状态"
            :disabled="submitLoading"
          >
            <a-select-option :value="true">
              <a-icon type="check-circle" style="color: #52c41a; margin-right: 8px;"/>
              {{ $t('status.enable') }}
            </a-select-option>
            <a-select-option :value="false">
              <a-icon type="stop" style="color: #ff4d4f; margin-right: 8px;"/>
              {{ $t('status.disable') }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="详细描述" prop="description" style="margin-bottom: 12px;">
          <a-tooltip :title="form.description">
            <a-input
              v-model="form.description"
              placeholder="请输入详细描述"
              type="textarea"
              allow-clear
              class="desc-ellipsis"
              :rows="3"
              :disabled="submitLoading"/>
          </a-tooltip>
        </a-form-model-item>
        <!-- <a-form-model-item label="配置内容" prop="configuration" style="margin-bottom: 24px; margin-top: 32px;">
          <div class="config-editor-wrapper">
            <div class="config-toolbar">
              <a-space>
                <a-button size="small" @click="loadDefaultConfig" :disabled="!form.type">
                  <a-icon type="file-text" />
                  加载默认配置
                </a-button>
                <a-button size="small" @click="formatConfig">
                  <a-icon type="form" />
                  格式化
                </a-button>
                <a-button size="small" @click="validateConfig">
                  <a-icon type="check-circle" />
                  验证配置
                </a-button>
              </a-space>
            </div>
            <div class="editor-adaptive">
              <CodeEditor
                v-model="form.configuration"
                :height="300"
                language="json"
                @input="onCodeEditorInput"
                @change="onCodeEditorChange"
                @blur="onCodeEditorBlur"
                @focus="onCodeEditorFocus"
              />
            </div>
          </div>
        </a-form-model-item> -->
      </a-form-model>
      <div class="bottom-control-modal">
        <a-space>
          <a-button type="primary" @click="submitForm" :loading="submitLoading" icon="save"
                    v-hasPermi="getSubmitPermissions()">
            保存
          </a-button>
          <a-button type="dashed" @click="cancel" :disabled="submitLoading" icon="close">
            取消
          </a-button>
        </a-space>
      </div>
    </a-spin>
  </a-modal>
</template>

<script>
import { queryProductList } from '@/api/system/dev/product';
import { addNetwork, updateNetwork, validateNetwork } from '@/api/system/network';
import CodeEditor from '@/components/CodeEditor.vue';

export default {
  name: 'CreateForm',
  props: {
    networkTypeOptions: {
      type: Array,
      required: true
    },
    productOptions: {
      type: Array,
      required: true
    },
    usedProductKeys: {
      type: Array,
      default: () => []
    }
  },
  components: {
    CodeEditor
  },
  data() {
    return {
      loading: false,
      submitLoading: false,
      formTitle: '',
      // 表单参数
      form: {
        id: undefined,
        type: undefined,
        unionId: undefined,
        productKey: '',
        productName: '',
        name: undefined,
        description: undefined,
        configuration: '{}',
        state: false
      },
      // 1增加,2修改
      formType: 1,
      open: false,
      // 产品搜索相关
      productSearchList: [],
      productSearchLoading: false,
      productSearchTimer: null,
      // 配置模板相关
      configTemplateVisible: false,
      templateActiveKey: 'tcp_server',
      // WebSocket 配置
      wsConfig: {
        host: '',
        port: 9001,
        path: '/ws',
        clientId: '',
        username: '',
        password: '',
        subProtocol: '', // 子协议（如mqtt, stomp等）
        topics: '',
        maxConnections: 1000,
        allowOrigins: true
      },
      // 配置模板
      webSocketTemplate: `{
  "host": "",
  "port": 9001,
  "path": "/ws",
  "clientId": "",
  "username": "",
  "password": "",
  "subProtocol": "",
  "topics": "",
  "maxConnections": 1000,
  "allowOrigins": true
}`,
      tcpServerTemplate: `{
  "allIdleTime": 0,
  "allowInsert": false,
  "alwaysPreDecode": false,
  "decodeType": "STRING",
  "host": "0.0.0.0",
  "idleInterval": 0,
  "onlyCache": false,
  "parserConfiguration": {
    "byteOrderLittle": true,
    "delimiter": "]",
    "delimitedMaxlength": 1024,
    "failFast": true
  },
  "parserType": "DELIMITER",
  "readerIdleTime": 360,
  "readTimeout": 0,
  "sendTimeout": 0,
  "ssl": false,
  "writerIdleTime": 0
}`,
      tcpClientTemplate: `{
  "host": "127.0.0.1",
  "ssl": false,
  "connectTimeout": 30,
  "readTimeout": 60,
  "writeTimeout": 60,
  "keepAlive": true,
  "reuseAddr": true,
  "tcpNoDelay": true
}`,
      mqttServerTemplate: `{
  "autoReconnect": true,
  "cleanSession": true,
  "clientIdPrefix": "univ_cli_",
  "defaultQos": 1,
  "enabled": true,
  "host": "tcp://localhost:1883",
  "id": "your_network_id",
  "keepAliveInterval": 60,
  "password": "your_password",
  "productKey": "your_product_key",
  "ssl": false,
  "subscribeTopics": "$univ_cli/up/property/+/+",
  "username": "univ_cli",
  "connectTimeout": 30
}`,
      mqttClientTemplate: `{
  "host": "tcp://localhost:1883",
  "port": 1883,
  "clientId": "your_client_id",
  "username": "your_username",
  "password": "your_password",
  "productKey": "your_product_key",
  "ssl": false,
  "cleanSession": true,
  "keepAliveInterval": 60,
  "connectTimeout": 30,
  "autoReconnect": true,
  "defaultQos": 1
}`,
      rules: {
        type: [{required: true, message: '请选择网络类型', trigger: 'submit'}],
        name: [{required: true, message: '请输入网络组件名称', trigger: 'submit'}],
        unionId: [{required: true, message: '请输入唯一标识', trigger: 'submit'}],
        productKey: [{required: true, message: '请选择产品', trigger: 'submit'}],
        configuration: [{required: true, message: '请输入配置内容', trigger: 'submit'}]
      }
    }
  },
  computed: {
    isTCP() {
      return this.form.type === 'TCP_CLIENT' || this.form.type === 'TCP_SERVER' || this.form.type === 'UDP'
    },
    isMQTT() {
      return this.form.type === 'MQTT_CLIENT' || this.form.type === 'MQTT_SERVER'
    },
    isWebSocket() {
      return this.form.type === 'WEB_SOCKET_SERVER' || this.form.type === 'WEB_SOCKET_CLIENT'
    },
    isWebSocketClient() {
      return this.form.type === 'WEB_SOCKET_CLIENT'
    },
    productKeyProxy: {
      get() {
        return (typeof this.form.productKey === 'string' || typeof this.form.productKey
          === 'number')
          ? this.form.productKey : ''
      },
      set(val) {
        this.form.productKey = (typeof val === 'string' || typeof val === 'number') ? val : ''
      }
    }
  },
  created() {
  },
  watch: {
    open(val) {
    }
  },
  methods: {
    /** 获取提交按钮所需的权限 */
    getSubmitPermissions() {
      // 根据网络类型和操作类型返回相应的权限标识
      const isAdd = this.formType === 1
      
      // 判断是 MQTT 还是 TCP/UDP
      if (this.isMQTT) {
        return [isAdd ? 'network:mqtt:add' : 'network:mqtt:edit']
      } else if (this.isTCP) {
        return [isAdd ? 'network:tcp:add' : 'network:tcp:edit']
      }
      
      // 默认返回 TCP 权限
      return [isAdd ? 'network:tcp:add' : 'network:tcp:edit']
    },
    // 远程产品搜索
    handleProductSearch(value) {
      if (this.productSearchTimer) {
        clearTimeout(this.productSearchTimer)
      }

      this.productSearchTimer = setTimeout(() => {
        this.productSearchLoading = true
        const params = value ? {name: value, pageSize: 50} : {pageSize: 50}

        queryProductList(params).then(response => {
          this.productSearchList = (response.rows || []).map(item => ({
            ...item,
            productKey: item.productKey ? String(item.productKey) : ''
          }))
          this.productSearchLoading = false
        }).catch(() => {
          this.productSearchList = []
          this.productSearchLoading = false
        })
      }, 300) // 防抖延迟
    },

    // 产品选择框聚焦
    handleProductFocus() {
      if (this.productSearchList.length === 0) {
        this.handleProductSearch('')
      }
    },

    // 网络类型变化处理
    handleTypeChange(value) {
      this.form.type = value
      // 当网络类型改变时，清空产品选择（因为不同网络类型可能需要不同的产品）
      this.form.productKey = ''
      // 如果是TCP类型，自动加载默认配置
      if (this.isTCP) {
        this.loadDefaultConfig()
      } else if (this.isWebSocket) {
        // WebSocket 初始化配置
        this.wsConfig = {
          port: 9001,
          path: '/ws',
          clientId: '',
          username: '',
          password: '',
          maxConnections: 1000,
          allowOrigins: true
        }
        this.syncWsConfigToForm()
      }
    },
    handleProductChange(value) {
      this.form.productKey = value
      // 当产品改变时，自动更新配置中的productKey
      if (value && this.form.configuration) {
        try {
          const config = JSON.parse(this.form.configuration)
          config.productKey = value
          this.form.configuration = JSON.stringify(config, null, 2)
        } catch (error) {
          // 如果配置不是有效的JSON，忽略更新
        }
      }
    },

    // 获取选中产品的名称
    getSelectedProductName() {
      const selected = this.productSearchList.find(item => item.productKey === this.form.productKey)
      return selected ? selected.name : ''
    },
    // 加载默认配置
    loadDefaultConfig() {
      if (!this.form.type) {
        this.$message.warning('请先选择网络类型')
        return
      }

      let template = '{}'
      switch (this.form.type) {
        case 'TCP_SERVER':
          template = this.tcpServerTemplate
          break
        case 'TCP_CLIENT':
          template = this.tcpClientTemplate
          break
        case 'MQTT_SERVER':
          template = this.mqttServerTemplate
          break
        case 'MQTT_CLIENT':
          template = this.mqttClientTemplate
          break
        case 'WEB_SOCKET_SERVER':
        case 'WEB_SOCKET_CLIENT':
          template = this.webSocketTemplate
          break
      }

      try {
        // 替换产品Key
        const config = JSON.parse(template)
        if (this.form.productKey) {
          config.productKey = this.form.productKey
        }
        this.form.configuration = JSON.stringify(config, null, 2)
      } catch (error) {
        this.form.configuration = template
      }
    },
    // 格式化配置
    formatConfig() {
      try {
        this.form.configuration = JSON.stringify(JSON.parse(this.form.configuration), null, 2)
        this.$message.success('格式化成功')
      } catch (error) {
        this.$message.error('JSON格式错误，无法格式化')
      }
    },
    // 验证配置
    validateConfig() {
      try {
        // 调用后端验证接口
        validateNetwork({
          type: this.form.type,
          unionId: this.form.unionId,
          name: this.form.name,
          configuration: this.form.configuration
        }).then(response => {
          this.$message.success('配置验证通过')
        }).catch(error => {
          this.$message.error(error.msg || '配置验证失败')
        })
      } catch (error) {
        this.$message.error('JSON格式错误')
      }
    },
    // 显示配置模板
    showConfigTemplate() {
      this.configTemplateVisible = true
      // 根据当前选择的类型设置默认标签页
      if (this.form.type) {
        switch (this.form.type) {
          case 'TCP_SERVER':
            this.templateActiveKey = 'tcp_server'
            break
          case 'TCP_CLIENT':
            this.templateActiveKey = 'tcp_client'
            break
          case 'MQTT_SERVER':
            this.templateActiveKey = 'mqtt_server'
            break
          case 'MQTT_CLIENT':
            this.templateActiveKey = 'mqtt_client'
            break
        }
      }
    },
    // 使用选中的模板
    useSelectedTemplate() {
      let template = '{}'
      switch (this.templateActiveKey) {
        case 'tcp_server':
          template = this.tcpServerTemplate
          break
        case 'tcp_client':
          template = this.tcpClientTemplate
          break
        case 'mqtt_server':
          template = this.mqttServerTemplate
          break
        case 'mqtt_client':
          template = this.mqttClientTemplate
          break
      }

      try {
        // 替换产品Key
        const config = JSON.parse(template)
        if (this.form.productKey) {
          config.productKey = this.form.productKey
        }
        this.form.configuration = JSON.stringify(config, null, 2)
        this.configTemplateVisible = false
        this.$message.success('模板已应用')
      } catch (error) {
        this.form.configuration = template
        this.configTemplateVisible = false
        this.$message.success('模板已应用')
      }
    },
    // 代码编辑器事件
    onCodeEditorInput(value) {
      this.form.configuration = value
    },
    onCodeEditorChange(value) {
      this.form.configuration = value
    },
    onCodeEditorBlur() {
      // 失焦时自动格式化
      this.formatConfig()
    },
    onCodeEditorFocus() {
      // 聚焦时的处理
    },
    // 新增
    handleAdd() {
      this.reset()
      this.formType = 1
      this.formTitle = '新增网络组件'
      this.open = true
      this.form.productKey = ''
    },
    // 修改
    handleUpdate(row) {
      this.reset()
      this.formType = 2
      this.formTitle = '修改网络组件'
      this.open = true

      // 确保数据类型一致性，特别是state字段
      this.form = Object.assign({}, row, {
        state: Boolean(row.state),  // 强制转换为布尔值
        id: row.id || undefined,
        type: row.type || undefined,
        unionId: row.unionId || undefined,
        productKey: row.productKey || '',
        productName: row.productName || '',
        name: row.name || undefined,
        description: row.description || undefined,
        configuration: row.configuration || '{}'
      })

      // 确保配置是格式化的JSON
      if (this.form.configuration) {
        try {
          const config = JSON.parse(this.form.configuration)
          this.form.configuration = JSON.stringify(config, null, 2)
          
          // 如果是 WebSocket 类型，解析配置到 wsConfig
          if (this.isWebSocket) {
            this.parseWsConfigFromForm()
          }
        } catch (error) {
          // 如果解析失败，保持原样
        }
      }

      // 调试信息
      console.log('修改时的原始数据:', row)
      console.log('修改时的表单数据:', this.form)
      console.log('state值:', this.form.state, '类型:', typeof this.form.state)
    },
    // 提交
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.submitLoading = true
          // 验证JSON格式
          try {
            JSON.parse(this.form.configuration)
          } catch (error) {
            this.$message.error('配置内容必须是有效的JSON格式')
            this.submitLoading = false
            return
          }

          // 确保数据类型一致性，特别是state字段
          const submitData = {
            ...this.form,
            state: Boolean(this.form.state),  // 强制转换为布尔值
            // 确保其他字段的数据类型
            id: this.form.id || undefined,
            type: this.form.type || undefined,
            unionId: this.form.unionId || undefined,
            productKey: this.form.productKey || '',
            productName: this.form.productName || '',
            name: this.form.name || undefined,
            description: this.form.description || undefined,
            configuration: this.form.configuration || '{}'
          }

          // 调试信息
          console.log('原始表单数据:', this.form)
          console.log('提交的表单数据:', submitData)
          console.log('state值:', submitData.state, '类型:', typeof submitData.state)

          const api = this.formType === 1 ? addNetwork : updateNetwork
          api(submitData).then(response => {
            this.$message.success('保存成功')
            this.open = false
            this.$emit('ok')
          }).catch(error => {
            // 处理错误响应，优先显示服务器返回的错误信息
            let errorMsg = '保存失败'
            if (error.response && error.response.data) {
              errorMsg = error.response.data.msg || error.response.data.message || errorMsg
            } else if (error.data && error.data.msg) {
              errorMsg = error.data.msg
            } else if (error.message) {
              errorMsg = error.message
            }
            this.$message.error(errorMsg, 5)
          }).finally(() => {
            this.submitLoading = false
          })
        }
      })
    },
    // 取消
    cancel() {
      this.open = false
      this.reset()
    },
    // 重置
    reset() {
      this.form = {
        id: undefined,
        type: undefined,
        unionId: undefined,
        productKey: '',
        productName: '',
        name: undefined,
        description: undefined,
        configuration: '{}',
        state: false
      }
      this.$nextTick(() => {
        this.$refs.form.clearValidate()
      })
    },
    // 关闭
    onClose() {
      this.open = false
      this.reset()
    },
    // 抽屉显示状态改变
    handleDrawerVisibleChange(visible) {
      if (!visible) {
        this.reset()
      }
    },
    filterProductOption(input, option) {
      // 支持按产品名称和ProductKey进行模糊搜索
      const searchText = input.toLowerCase()
      const productName = (option.label || '').toLowerCase()
      const productKey = (option.value || '').toLowerCase()
      return productName.indexOf(searchText) >= 0 || productKey.indexOf(searchText) >= 0
    },
    /** 加载产品名称搜索列表 */
    async loadProductNameSearchList(searchValue = '') {
      try {
        this.productSearchLoading = true
        const params = {
          pageNum: 1,
          pageSize: 50, // 限制返回数量
          name: searchValue, // 根据名称搜索
          state: 0 // 只搜索发布状态的产品
        }
        const response = await queryProductList(params)
        this.productSearchList = (response.rows || []).map(item => ({
          ...item,
          productKey: item.productKey ? String(item.productKey) : ''
        }))
      } catch (error) {
        console.error('加载产品搜索列表失败:', error)
        this.$message.error('加载产品搜索列表失败')
      } finally {
        this.productSearchLoading = false
      }
    },
    handleProductNameSearch(value) {
      this.loadProductNameSearchList(value)
    },
    handleProductNameFocus() {
      if (this.productSearchList.length === 0) {
        this.loadProductNameSearchList('')
      }
    },
    handleProductNameChange(value) {
      const selected = this.productSearchList.find(
        item => String(item.productKey) === String(value))
      this.form.productKey = value ? String(value) : ''
      this.form.productName = selected ? selected.name : ''
    },
    // WebSocket 配置同步到表单
    syncWsConfigToForm() {
      this.form.configuration = JSON.stringify(this.wsConfig, null, 2)
    },
    // 从表单配置解析到 WebSocket 配置
    parseWsConfigFromForm() {
      if (this.isWebSocket && this.form.configuration) {
        try {
          const config = JSON.parse(this.form.configuration)
          this.wsConfig = {
            host: config.host || '',
            port: config.port || 9001,
            path: config.path || '/ws',
            clientId: config.clientId || '',
            username: config.username || '',
            password: config.password || '',
            subProtocol: config.subProtocol || '',
            topics: config.topics || '',
            maxConnections: config.maxConnections || 1000,
            allowOrigins: config.allowOrigins !== undefined ? config.allowOrigins : true
          }
        } catch (error) {
          console.error('解析 WebSocket 配置失败:', error)
        }
      }
    }
  },
  watch: {
    // 监听 WebSocket 配置变化，自动同步到表单
    wsConfig: {
      handler(val) {
        if (this.isWebSocket && this.open) {
          this.syncWsConfigToForm()
        }
      },
      deep: true
    }
  }
}
</script>

<style lang="less" scoped>
.type-cell {
  display: flex;
  align-items: center;
}

.product-option {
  display: flex;
  align-items: center;

  .product-info {
    display: flex;
    flex-direction: column;

    .product-name {
      font-weight: 500;
      color: #262626;
    }

    .product-key {
      font-size: 12px;
      color: #999;
    }
  }
}

.config-editor-wrapper {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;

  .config-toolbar {
    background: #fafafa;
    padding: 8px 12px;
    border-bottom: 1px solid #d9d9d9;
  }

  .editor-adaptive {
    background: #fff;
  }
}

.config-template {
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #24292e;
  overflow-x: auto;
  max-height: 400px;
}

.bottom-control {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.desc-ellipsis {
  resize: vertical;
}

.ellipsis {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
  color: #999;
  font-size: 12px;
}

.product-key-selected {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
  line-height: 1.5;
  padding-left: 2px;
}

.product-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.product-name {
  font-size: 14px;
  color: #222;
  font-weight: 500;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-key-in-dropdown {
  color: #bbb;
  font-size: 12px;
  margin-left: 12px;
  flex-shrink: 0;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.bottom-control-modal {
  margin-top: 32px;
  text-align: center;
}

// 产品选择下拉框样式
:deep(.product-key-dropdown) {
  .ant-select-dropdown-menu-item {
    padding: 8px 12px;

    &:hover {
      background-color: #f5f5f5;
    }
  }
}

.product-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 20px;
}

.product-name {
  font-size: 14px;
  color: #222;
  font-weight: 500;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-key-in-dropdown {
  color: #bbb;
  font-size: 12px;
  margin-left: 12px;
  flex-shrink: 0;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.product-search-option {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.product-search-name {
  font-weight: 500;
  color: #262626;
}

.product-search-key {
  font-size: 12px;
  color: #999;
}
</style>
