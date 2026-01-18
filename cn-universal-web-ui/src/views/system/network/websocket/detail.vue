<template>
  <div class="app-container">
    <a-card :bordered="false">
      <div class="page-header">
        <div class="header-left">
          <a-button type="text" icon="left" @click="$router.back()" class="back-btn" />
          <div class="page-title">
            <h1>{{ networkInfo.name || 'WebSocket网络组件' }}</h1>
          </div>
          <a-tag :color="networkInfo.running ? 'green' : 'red'" style="margin-left: 12px;">
            {{ networkInfo.running ? '运行中' : '已停止' }}
          </a-tag>
        </div>
        <div class="header-right">
          <a-button 
            v-if="!editing" 
            type="primary" 
            icon="edit" 
            @click="startEdit" 
            v-hasPermi="['network:websocket:edit']">
            编辑配置
          </a-button>
          <template v-else>
            <a-button @click="cancelEdit" style="margin-right: 8px;">取消</a-button>
            <a-button type="primary" @click="handleSaveAll" :loading="saving" v-hasPermi="['network:websocket:edit']">
              保存配置
            </a-button>
          </template>
        </div>
      </div>
      
      <a-spin :spinning="loading" tip="Loading...">
        <!-- 自定义标签页导航 -->
        <div class="custom-tabs-container">
          <div class="custom-tabs-nav">
            <div class="custom-tab-item" :class="{ active: activeTab === '1' }" @click="switchTab('1')">
              <a-icon type="info-circle" style="margin-right: 6px;" />
              基础配置
            </div>
            <div class="custom-tab-item" :class="{ active: activeTab === '2' }" @click="switchTab('2')">
              <a-icon type="api" style="margin-right: 6px;" />
              高级设置
            </div>
          </div>

          <!-- 标签页内容 -->
          <div class="custom-tab-content">
            <!-- 基础配置 -->
            <div v-show="activeTab === '1'" class="tab-pane">
              <div class="device-basic-info">
                <div class="basic-info-header">
                  <h3>基础配置</h3>
                  <a-button v-if="!editing" type="link" size="small" @click="startEdit" v-hasPermi="['network:websocket:edit']">
                    <a-icon type="edit" /> 编辑
                  </a-button>
                </div>
                
                <!-- 基础信息部分 -->
                <div class="info-section">
                  <div class="section-subtitle">基础信息</div>
                  <div class="basic-info-grid compact-grid">
                    <div class="info-item">
                      <span class="info-label">组件类型</span>
                      <a-tag color="blue">WebSocket</a-tag>
                    </div>
                    <div class="info-item">
                      <span class="info-label">创建时间</span>
                      <span class="info-value">{{ parseTime(networkInfo.createDate) }}</span>
                    </div>
                    <div class="info-item info-item-full">
                      <span class="info-label">唯一标识</span>
                      <div class="info-value-group">
                        <span class="info-value code">{{ networkInfo.unionId }}</span>
                        <a-button type="text" size="small" class="copy-action-btn" @click.stop="copyToClipboard(networkInfo.unionId)" title="复制">
                          <a-icon type="copy"/>
                        </a-button>
                      </div>
                    </div>
                    <div class="info-item info-item-full" v-if="networkInfo.description">
                      <span class="info-label">描述信息</span>
                      <span class="info-value">{{ networkInfo.description }}</span>
                    </div>
                  </div>
                </div>

                <!-- 连接配置部分 -->
                <div class="info-section" style="margin-top: 24px;">
                  <div class="section-subtitle">连接配置</div>
                  <template v-if="!editing">
                    <div class="basic-info-grid compact-grid">
                      <div class="info-item" v-for="field in connectionFields" :key="field.key" v-if="!field.hide">
                        <span class="info-label">{{ field.label }}</span>
                        <span class="info-value">{{ renderReadValue(field) }}</span>
                      </div>
                    </div>
                  </template>
                  
                  <template v-else>
                    <a-form :model="formData" layout="vertical">
                      <a-row :gutter="[16, 0]">
                        <a-col :span="12" v-for="field in connectionFields" :key="field.key" v-if="!field.hide">
                          <a-form-item :label="field.label" :required="field.required">
                            <a-input
                              v-if="field.type === 'string' && field.key !== 'password'"
                              v-model="formData[field.key]"
                              :placeholder="field.remark"
                            />
                            <a-input-password
                              v-else-if="field.key === 'password'"
                              v-model="formData[field.key]"
                              :placeholder="field.remark"
                            />
                            <a-input-number
                              v-else-if="field.type === 'int'"
                              v-model="formData[field.key]"
                              :placeholder="field.remark"
                              style="width:100%"
                              :step="1"
                              :precision="0"
                              :min="field.min"
                              :max="field.max"
                            />
                            <a-select
                              v-else-if="field.type === 'select'"
                              v-model="formData[field.key]"
                              :placeholder="field.remark"
                            >
                              <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                              </a-select-option>
                            </a-select>
                            <a-switch
                              v-else-if="field.type === 'boolean'"
                              v-model="formData[field.key]"
                            />
                          </a-form-item>
                        </a-col>
                      </a-row>
                    </a-form>
                  </template>
                </div>
              </div>
            </div>

            <!-- 高级设置 -->
            <div v-show="activeTab === '2'" class="tab-pane">
              <div class="device-basic-info">
                <div class="basic-info-header">
                  <h3>高级设置</h3>
                  <a-button v-if="!editing" type="link" size="small" @click="startEdit" v-hasPermi="['network:websocket:edit']">
                    <a-icon type="edit" /> 编辑
                  </a-button>
                </div>
                
                <template v-if="!editing">
                  <div class="basic-info-grid">
                    <div class="info-item" v-for="field in advancedFields" :key="field.key" v-if="!field.hide">
                      <span class="info-label">{{ field.label }}</span>
                      <span class="info-value">{{ renderReadValue(field) }}</span>
                    </div>
                  </div>
                </template>
                
                <template v-else>
                  <a-form :model="formData" layout="vertical">
                    <a-row :gutter="16">
                      <a-col :span="12" v-for="field in advancedFields" :key="field.key" v-if="!field.hide">
                        <a-form-item :label="field.label">
                          <template v-if="field.type === 'int'">
                            <a-input-number v-model="formData[field.key]" :placeholder="field.remark" style="width:100%" :step="1" :precision="0"/>
                          </template>
                          <template v-else-if="field.type === 'json'">
                            <a-textarea v-model="formData[field.key]" :placeholder="field.remark" :rows="3" />
                          </template>
                          <template v-else-if="field.type === 'select'">
                            <a-select v-model="formData[field.key]" :placeholder="field.remark">
                              <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                              </a-select-option>
                            </a-select>
                          </template>
                          <template v-else-if="field.type === 'boolean'">
                            <a-select v-model="formData[field.key]" :placeholder="field.remark">
                              <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                              </a-select-option>
                            </a-select>
                          </template>
                          <template v-else>
                            <a-input v-model="formData[field.key]" :placeholder="field.remark"/>
                          </template>
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </a-form>
                </template>
              </div>
            </div>
          </div>
        </div>
      </a-spin>

      <!-- 操作按钮 -->
      <div class="operation-buttons">
        <a-space>
          <a-button @click="handleToggleState" v-if="!editing" v-hasPermi="['network:websocket:start', 'network:websocket:stop']">
            <a-icon :type="networkInfo.running ? 'pause-circle' : 'play-circle'" />
            {{ networkInfo.running ? '停止' : '启动' }}
          </a-button>
          <a-button v-if="!editing" @click="handleRestart" v-hasPermi="['network:websocket:restart']">
            <a-icon type="reload" />
            重启
          </a-button>
          <a-button @click="copyConfig" v-if="!editing">
            <a-icon type="copy" />
            复制配置
          </a-button>
          <a-button @click="downloadConfig" v-if="!editing">
            <a-icon type="download" />
            下载配置
          </a-button>
        </a-space>
      </div>
    </a-card>
  </div>
</template>

<script>
import { getNetwork, restartNetwork, startNetwork, stopNetwork, updateNetwork } from '@/api/system/network';
import { parseTime } from '@/utils/ruoyi';

export default {
  name: 'WebSocketNetworkDetail',
  data() {
    return {
      loading: false,
      saving: false,
      networkInfo: {
        id: undefined,
        type: undefined,
        unionId: undefined,
        productKey: undefined,
        name: undefined,
        description: undefined,
        configuration: '{}',
        running: false,
        createDate: undefined
      },
      connectionFields: [],
      advancedFields: [],
      formData: {},
      editing: false,
      formDataBackup: {},
      activeTab: '1'
    }
  },
  created() {
    this.getNetworkDetail()
  },
  methods: {
    parseTime,
    /** 获取WebSocket网络组件详情 */
    getNetworkDetail() {
      const id = this.$route.params.id
      if (!id) {
        this.$message.error('WebSocket网络组件ID不能为空')
        this.goBack()
        return
      }
      this.loading = true
      getNetwork(id).then(async response => {
        this.networkInfo = response.data
        this.loading = false
        await this.loadConfigFields()
      }).catch(() => {
        this.loading = false
        this.goBack()
      })
    },
    /** 加载配置字段 */
    async loadConfigFields() {
      // 判断是客户端还是服务端
      const isClient = this.networkInfo.type === 'WEB_SOCKET_CLIENT';
      
      // 连接配置字段（必需）
      this.connectionFields = [
        {
          key: 'host',
          label: '服务器地址',
          remark: '例如: 192.168.1.100 或 ws://example.com',
          type: 'string',
          required: true,
          hide: !isClient, // 只在客户端模式显示
          default: ''
        },
        {
          key: 'port',
          label: isClient ? '服务器端口' : '监听端口',
          remark: '例如: 9001',
          type: 'int',
          required: true,
          hide: false,
          default: 9001,
          min: 1,
          max: 65535
        },
        {
          key: 'path',
          label: '路径',
          remark: '例如: /ws (必须以/开头)',
          type: 'string',
          required: true,
          hide: false,
          default: '/ws'
        },
        {
          key: 'clientId',
          label: 'ClientId',
          remark: '客户端ID，不填则自动生成UUID',
          type: 'string',
          required: false,
          hide: false,
          default: ''
        },
        {
          key: 'username',
          label: '用户名',
          remark: '认证用户名（可选）',
          type: 'string',
          required: false,
          hide: false,
          default: ''
        },
        {
          key: 'password',
          label: '密码',
          remark: '认证密码（可选）',
          type: 'password',
          required: false,
          hide: false,
          default: ''
        },
        {
          key: 'subProtocol',
          label: '子协议',
          remark: 'WebSocket子协议（Sec-WebSocket-Protocol），如: mqtt, stomp, wamp等',
          type: 'string',
          required: false,
          hide: this.networkInfo && this.networkInfo.type !== 'WEB_SOCKET_CLIENT',
          default: ''
        },
        {
          key: 'topics',
          label: '订阅主题',
          remark: '多个主题用逗号分隔，如: sensor/temperature,sensor/humidity,device/status',
          type: 'string',
          required: false,
          hide: this.networkInfo && this.networkInfo.type !== 'WEB_SOCKET_CLIENT',
          default: ''
        },
        {
          key: 'maxConnections',
          label: '最大连接数',
          remark: '允许的最大并发连接数',
          type: 'int',
          required: false,
          hide: false,
          default: 1000,
          min: 1,
          max: 100000
        },
        {
          key: 'allowOrigins',
          label: '允许跨域',
          remark: '是否允许所有来源的跨域请求',
          type: 'boolean',
          required: false,
          hide: false,
          default: true,
          options: [
            {label: '是', value: true},
            {label: '否', value: false}
          ]
        }
      ]

      // 高级配置字段
      this.advancedFields = [
        {
          key: 'ssl',
          label: '启用SSL/TLS',
          remark: '是否启用安全连接',
          type: 'boolean',
          required: false,
          hide: false,
          default: false,
          options: [
            {label: '是', value: true},
            {label: '否', value: false}
          ]
        },
        {
          key: 'maxFramePayloadLength',
          label: '最大帧长度(字节)',
          remark: '单个WebSocket帧的最大长度',
          type: 'int',
          required: false,
          hide: false,
          default: 1048576,
          min: 4096,
          max: 134217728
        },
        {
          key: 'idleTimeout',
          label: '空闲超时(秒)',
          remark: '连接空闲多久后断开',
          type: 'int',
          required: false,
          hide: false,
          default: 0,
          min: 0
        },
        {
          key: 'threadPoolSize',
          label: '线程池大小',
          remark: '处理连接的线程数',
          type: 'int',
          required: false,
          hide: false,
          default: 10,
          min: 1,
          max: 1000
        }
      ]

      // 加载现有配置
      let config = {}
      try {
        config = JSON.parse(this.networkInfo.configuration)
      } catch {
        // 配置格式错误，使用空对象
      }

      // 合并所有字段
      const allFields = [...this.connectionFields, ...this.advancedFields]
      allFields.forEach(f => {
        const val = config[f.key]
        this.$set(this.formData, f.key,
          val !== undefined ? val : f.default !== undefined ? f.default : '')
      })

      // 备份初始值用于取消恢复
      this.formDataBackup = JSON.parse(JSON.stringify(this.formData))
    },
    /** 开始编辑 */
    startEdit() {
      this.editing = true
      this.formDataBackup = JSON.parse(JSON.stringify(this.formData))
    },
    /** 取消编辑 */
    cancelEdit() {
      this.editing = false
      this.formData = JSON.parse(JSON.stringify(this.formDataBackup))
    },
    /** 保存所有配置 */
    handleSaveAll() {
      this.handleSaveConfig()
    },
    /** 保存配置 */
    handleSaveConfig() {
      // 验证必填字段
      const allFields = [...this.connectionFields, ...this.advancedFields]
      for (const field of allFields) {
        if (field.required && !this.formData[field.key]) {
          this.$message.error(`${field.label}不能为空`)
          return
        }
      }

      this.saving = true
      const config = {}
      allFields.forEach(f => {
        config[f.key] = this.formData[f.key]
      })

      const data = {
        ...this.networkInfo,
        configuration: JSON.stringify(config)
      }

      updateNetwork(data).then(response => {
        this.$message.success('保存配置成功')
        this.editing = false
        this.saving = false
        this.getNetworkDetail()
      }).catch(error => {
        this.$message.error(error.msg || '保存配置失败')
        this.saving = false
      })
    },
    /** 启动/停止 */
    handleToggleState() {
      const action = this.networkInfo.running ? '停止' : '启动'
      this.$confirm({
        title: '确认操作',
        content: `确定要${action}WebSocket网络组件"${this.networkInfo.name}"吗？`,
        onOk: () => {
          const api = this.networkInfo.running ? stopNetwork : startNetwork
          api(this.networkInfo.id).then(response => {
            this.$message.success(`${action}成功`)
            this.getNetworkDetail()
          }).catch(error => {
            this.$message.error(error.msg || `${action}失败`)
          })
        }
      })
    },
    /** 重启 */
    handleRestart() {
      this.$confirm({
        title: '确认操作',
        content: `确定要重启WebSocket网络组件"${this.networkInfo.name}"吗？`,
        onOk: () => {
          restartNetwork(this.networkInfo.id).then(response => {
            this.$message.success('重启成功')
            this.getNetworkDetail()
          }).catch(error => {
            this.$message.error(error.msg || '重启失败')
          })
        }
      })
    },
    /** 复制配置 */
    copyConfig() {
      try {
        const config = JSON.parse(this.networkInfo.configuration)
        const configStr = JSON.stringify(config, null, 2)
        navigator.clipboard.writeText(configStr).then(() => {
          this.$message.success('配置已复制到剪贴板')
        }).catch(() => {
          this.$message.error('复制失败')
        })
      } catch (error) {
        this.$message.error('配置格式错误')
      }
    },
    /** 下载配置 */
    downloadConfig() {
      try {
        const config = JSON.parse(this.networkInfo.configuration)
        const configStr = JSON.stringify(config, null, 2)
        const blob = new Blob([configStr], {type: 'application/json'})
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `${this.networkInfo.name}_config.json`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
        this.$message.success('配置已下载')
      } catch (error) {
        this.$message.error('配置格式错误或下载失败')
      }
    },
    /** 复制到剪贴板 */
    copyToClipboard(text) {
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },
    /** 返回 */
    goBack() {
      this.$router.go(-1)
    },
    /** 切换标签页 */
    switchTab(tabKey) {
      this.activeTab = tabKey
    },
    /** 渲染读取值 */
    renderReadValue(field) {
      const val = this.formData[field.key]
      
      // 密码字段特殊处理
      if (field.key === 'password' || field.type === 'password') {
        return val ? '******' : '-'
      }
      
      // ClientId 特殊处理
      if (field.key === 'clientId') {
        return val || '自动生成'
      }
      
      if (field.type === 'boolean') {
        return val === true || val === 'true' ? '是' : '否'
      }
      if (field.type === 'int') {
        return val !== undefined && val !== null ? val : '-'
      }
      if (field.type === 'json') {
        try {
          return JSON.stringify(JSON.parse(val), null, 2)
        } catch {
          return val || '-'
        }
      }
      return val || '-'
    }
  }
}
</script>

<style lang="less" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;

    .back-btn {
      font-size: 20px;
      cursor: pointer;
      color: #1890ff;

      &:hover {
        color: #40a9ff;
      }
    }

    .page-title {
      h1 {
        margin: 0;
        font-size: 24px;
        font-weight: 500;
      }
    }
  }

  .header-right {
    display: flex;
    gap: 8px;
  }
}

.custom-tabs-container {
  margin-top: 24px;
}

.custom-tabs-nav {
  display: flex;
  gap: 16px;
  border-bottom: 2px solid #f0f0f0;
  margin-bottom: 16px;
}

.custom-tab-item {
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  color: #666;

  &.active {
    color: #1890ff;
    border-bottom-color: #1890ff;
  }

  &:hover:not(.active) {
    color: #1890ff;
  }
}

.custom-tab-content {
  margin-top: 24px;
}

.tab-pane {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.device-basic-info {
  padding: 24px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;

  .basic-info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .info-section {
    .section-subtitle {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e8e8e8;
    }
  }
}

.basic-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  &.compact-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &.info-item-full {
    grid-column: 1 / -1;
  }

  .info-label {
    font-size: 12px;
    color: #999;
    font-weight: 500;
  }

  .info-value {
    font-size: 14px;
    color: #333;
    word-break: break-all;

    &.code {
      font-family: 'Courier New', Courier, monospace;
      background: #f5f5f5;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }
  }

  .info-value-group {
    display: flex;
    align-items: center;
    gap: 8px;

    .copy-action-btn {
      padding: 0;
      height: auto;
      color: #1890ff;

      &:hover {
        color: #40a9ff;
      }
    }
  }
}

.operation-buttons {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 768px) {
  .basic-info-grid {
    grid-template-columns: 1fr;

    &.compact-grid {
      grid-template-columns: 1fr;
    }
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;

    .header-right {
      width: 100%;
    }
  }
}
</style>
