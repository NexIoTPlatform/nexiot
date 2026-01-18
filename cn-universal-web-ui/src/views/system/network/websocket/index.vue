<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <!-- 条件搜索 -->
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="24">
            <a-col :lg="6" :md="8" :sm="12" :xs="24">
              <a-form-item label="组件名称" prop="name">
                <a-input v-model.trim="queryParam.name" placeholder="请输入WebSocket组件名称"
                         allow-clear>
                  <a-icon slot="prefix" type="tag"/>
                </a-input>
              </a-form-item>
            </a-col>
            <a-col :lg="6" :md="8" :sm="12" :xs="24">
              <a-form-item :label="$t('common.running.status')" prop="running">
                <a-select placeholder="请选择运行状态" style="width: 100%"
                          v-model="queryParam.running" allow-clear>
                  <a-select-option :value="true">
                    <a-icon type="check-circle" style="color: #52c41a; margin-right: 8px;"/>
                    运行中
                  </a-select-option>
                  <a-select-option :value="false">
                    <a-icon type="stop" style="color: #ff4d4f; margin-right: 8px;"/>
                    已停止
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :lg="6" :md="8" :sm="12" :xs="24">
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="handleQuery" icon="search">{{ $t('button.query') }}</a-button>
                <a-button style="margin-left: 8px" @click="resetQuery" icon="reload">{{ $t('button.reset') }}</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <!-- 操作 -->
      <div class="table-operations">
        <a-space>
          <a-button type="primary" @click="$refs.createForm.handleAdd()"
                    v-hasPermi="['network:websocket:add']" icon="plus">
            {{ $t('button.add') }}</a-button>
          <a-button
            type="danger"
            @click="handleDelete"
            v-hasPermi="['network:websocket:remove']"
            ghost
            icon="delete">
            {{ $t('button.delete') }}</a-button>
          <a-button @click="handleExport" v-hasPermi="['network:websocket:export']" icon="export">
            {{ $t('button.export') }}</a-button>
        </a-space>
        <a-button
          type="dashed"
          shape="circle"
          :loading="loading"
          icon="reload"
          @click="getList"
          class="refresh-btn"
        />
      </div>
      <!-- 增加修改 -->
      <create-form
        ref="createForm"
        :networkTypeOptions="websocketTypeOptions"
        :productOptions="productOptions"
        :usedProductKeys="list.map(item => String(item.productKey)).filter(Boolean)"
        @ok="getList"
      />

      <!-- 空状态 -->
      <a-empty v-if="!loading && list.length === 0" description="暂无WebSocket网络组件数据"/>

      <!-- 卡片网格 -->
      <a-row :gutter="16">
        <a-col :span="6" v-for="item in list" :key="item.id">
          <a-card hoverable class="network-card">
            <!-- WebSocket类型标识 - 右上角 -->
            <div class="websocket-type-badge">
              <span class="websocket-badge">WebSocket</span>
            </div>

            <div class="card-header">
              <span style="display:flex;align-items:center;">
                <a-badge :status="item.running ? 'success' : 'default'"
                         :class="{ 'breath-badge': item.running }"
                         style="margin-right:12px;font-size:18px;line-height:1;"/>
              </span>
              <a @click="handleView(item)" class="card-title">
                {{ getDisplayName(item) }}
              </a>
            </div>
            <div class="card-body">
              <div class="card-row">
                <a-icon type="api" style="margin-right:4px;"/>
                端口：
                <a-tooltip :title="getConfigValue(item, 'port')">{{
                    getConfigValue(item, 'port')
                  }}
                </a-tooltip>
              </div>
              <div class="card-row">
                <a-icon type="link" style="margin-right:4px;"/>
                路径：
                <a-tooltip :title="getConfigValue(item, 'path', '未配置')">{{
                    getConfigValue(item, 'path', '未配置')
                  }}
                </a-tooltip>
              </div>
              <div class="card-row">
                <a-icon type="idcard" style="margin-right:4px;"/>
                ClientId：
                <a-tooltip :title="getConfigValue(item, 'clientId', '自动生成')">{{
                    getConfigValue(item, 'clientId', '自动生成')
                  }}
                </a-tooltip>
              </div>
              <div class="card-row">
                <a-icon type="link" style="margin-right:4px;"/>
                绑定产品：<span class="product-count-inline"><a-button
                v-if="item.productKey"
                type="link" size="small" @click="showBindProduct(item)"
                class="product-count-btn">
                  1 个
                </a-button><span v-else class="no-bind-text">
                  0 个（未绑定）
                </span></span>
              </div>
              <div class="card-row">
                <a-icon type="poweroff" style="margin-right:4px;"/>
                {{$t('common.status')}}：
                <span :class="getStatusClass(item)">
                  {{ getStatusText(item) }}
                </span>
              </div>
            </div>
            <div class="card-actions">
              <div class="action-btn start-btn"
                   @click="!isWebSocketConfigured(item) || item.running ? null : handleStart(item)"
                   v-hasPermi="['network:websocket:start']"
                   :class="{ disabled: !isWebSocketConfigured(item) || item.running }">
                <a-icon type="play-circle"/>
              </div>
              <div class="action-btn stop-btn"
                   @click="!isWebSocketConfigured(item) || !item.running ? null : handleStop(item)"
                   v-hasPermi="['network:websocket:stop']"
                   :class="{ disabled: !isWebSocketConfigured(item) || !item.running }">
                <a-icon type="pause-circle"/>
              </div>
              <div class="action-btn edit-btn"
                   @click="$refs.createForm.handleUpdate(item)"
                   v-hasPermi="['network:websocket:edit']">
                <a-icon type="edit"/>
              </div>
              <div class="action-btn delete-btn"
                   @click="handleDelete(item)"
                   v-hasPermi="['network:websocket:remove']">
                <a-icon type="delete"/>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-pagination
        v-if="total > 0"
        class="ant-table-pagination"
        show-size-changer
        show-quick-jumper
        :current="queryParam.pageNum"
        :total="total"
        :page-size="queryParam.pageSize"
        :pageSizeOptions="['12', '24', '36', '48']"
        @change="changeSize"
        @showSizeChange="onShowSizeChange"
      />

      <!-- 绑定产品弹窗 -->
      <a-modal
        v-model="bindProductsVisible"
        title="绑定产品"
        :footer="null"
        width="600px">
        <div v-if="currentBindProducts && currentBindProducts.length > 0">
          <div v-for="(item, index) in currentBindProducts" :key="index" style="margin-bottom: 16px;">
            <div style="display: flex; gap: 16px;">
              <div style="flex: 1;">
                <a-descriptions :column="1" size="small">
                  <a-descriptions-item :label="$t('product.name')">
                    {{ item.name }}
                  </a-descriptions-item>
                  <a-descriptions-item label="ProductKey">
                    {{ item.productKey }}
                  </a-descriptions-item>
                  <a-descriptions-item label="产品描述" v-if="item.description">
                    {{ item.description }}
                  </a-descriptions-item>
                  <a-descriptions-item label="公司简称" v-if="item.companyNo">
                    {{ item.companyNo }}
                  </a-descriptions-item>
                  <a-descriptions-item :label="$t('device.node')" v-if="item.deviceNode">
                    {{ item.deviceNode }}
                  </a-descriptions-item>
                  <a-descriptions-item :label="$t('device.accessMethod')" v-if="item.thirdPlatform">
                    {{ item.thirdPlatform }}
                  </a-descriptions-item>
                </a-descriptions>
              </div>
              <div
                style="width: 150px; height: 150px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;">
                <div v-if="getProductImage(item)"
                     style="width: 100%; height: 100%; border: 1px solid #d9d9d9; border-radius: 8px; overflow: hidden;">
                  <img :src="getProductImage(item)"
                       style="width: 100%; height: 100%; object-fit: cover;"/>
                </div>
                <div v-else
                     style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border: 1px solid #d9d9d9; border-radius: 8px;">
                  <a-icon type="appstore" style="color: #d9d9d9; font-size: 48px;"/>
                </div>
              </div>
            </div>
            <div v-if="index < currentBindProducts.length - 1"
                 style="margin-top: 16px; border-bottom: 1px solid #f0f0f0;"></div>
          </div>
        </div>
        <a-empty v-else description="暂无绑定产品"/>
      </a-modal>
    </a-card>
  </page-header-wrapper>
</template>

<script>
import { listProduct } from '@/api/system/dev/product';
import { delNetwork, delNetworkBatch, listNetwork, startNetwork, stopNetwork } from '@/api/system/network';
import { parseTime } from '@/utils/ruoyi';
import CreateForm from '../modules/CreateForm';

export default {
  name: 'WebSocketNetwork',
  components: {
    CreateForm
  },
  data() {
    return {
      // 遮罩层
      loading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      advanced: false,
      // 总条数
      total: 0,
      // WebSocket网络组件表格数据
      list: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // WebSocket类型选项
      websocketTypeOptions: [
        {dictValue: 'WEB_SOCKET_SERVER', dictLabel: 'WebSocket服务端'},
        {dictValue: 'WEB_SOCKET_CLIENT', dictLabel: 'WebSocket客户端'},
      ],
      // 产品选项
      productOptions: [],
      // 产品搜索相关
      productSearchLoading: false,
      productSearchList: [],
      allProductsList: [], // 保存所有产品数据
      // 绑定产品弹窗
      bindProductVisible: false,
      currentNetwork: {},
      currentBindProduct: null,
      // 绑定产品弹窗
      bindProductsVisible: false,
      currentNetwork: {},
      currentBindProducts: [],
      // 查询参数
      queryParam: {
        pageNum: 1,
        pageSize: 12,
        type: undefined,
        name: undefined,
        productKey: undefined,
        running: undefined,
        unionId: undefined
      },
      // 表格列配置
      columns: [
        {
          title: '组件名称',
          dataIndex: 'name',
          scopedSlots: {customRender: 'name'},
          ellipsis: true,
          width: '28%'
        },
        {
          title: '产品Key',
          dataIndex: 'productKey',
          ellipsis: true,
          width: '32%'
        },
        {
          title: '唯一标识',
          dataIndex: 'unionId',
          ellipsis: true,
          width: '28%'
        },
        {
          title: this.$t('user.operation'),
          dataIndex: 'operation',
          scopedSlots: {customRender: 'operation'},
          width: '12%',
          align: 'center',
          fixed: false
        }
      ]
    }
  },
  created() {
    this.getList()
    this.getProductOptions()
  },
  methods: {
    parseTime,
    /** 查询WebSocket网络组件列表 */
    getList() {
      this.loading = true
      // 只查询WebSocket类型的网络组件
      const params = {...this.queryParam}
      if (!params.type) {
        params.type = ['WEB_SOCKET_CLIENT', 'WEB_SOCKET_SERVER']
      }

      listNetwork(params).then(response => {
        this.list = response.rows || []
        this.total = response.total || 0
        this.loading = false
        // 强制更新视图
        this.$forceUpdate()
      }).catch(() => {
        this.loading = false
      })
    },
    /** 获取产品选项 */
    getProductOptions() {
      listProduct({pageSize: 1000}).then(response => {
        this.productOptions = response.rows.map(item => ({
          value: item.productKey,
          label: item.name
        }))
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParam.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParam = {
        pageNum: 1,
        pageSize: 12,
        type: undefined,
        name: undefined,
        productKey: undefined,
        running: undefined,
        unionId: undefined
      }
      this.handleQuery()
    },
    onShowSizeChange(current, pageSize) {
      this.queryParam.pageSize = pageSize
      this.getList()
    },
    changeSize(current, pageSize) {
      this.queryParam.pageNum = current
      this.queryParam.pageSize = pageSize
      this.getList()
    },
    /** 多选框选中数据 */
    onSelectChange(selectedRowKeys, selectedRows) {
      this.ids = selectedRowKeys
      this.single = selectedRowKeys.length !== 1
      this.multiple = !selectedRowKeys.length
    },
    /** 启动WebSocket网络组件 */
    handleStart(row) {
      this.$confirm({
        title: '确认操作',
        content: `确定要启动WebSocket网络组件"${row.name}"吗？`,
        onOk: () => {
          startNetwork(row.id).then(response => {
            this.$message.success('启动成功')
            // 延迟一下再刷新，确保后端状态已更新
            setTimeout(() => {
              this.getList()
            }, 500)
          }).catch(error => {
            this.$message.error(error.msg || '启动失败')
          })
        }
      })
    },
    /** 停止WebSocket网络组件 */
    handleStop(row) {
      this.$confirm({
        title: '确认操作',
        content: `确定要停止WebSocket网络组件"${row.name}"吗？`,
        onOk: () => {
          stopNetwork(row.id).then(response => {
            this.$message.success('停止成功')
            // 延迟一下再刷新，确保后端状态已更新
            setTimeout(() => {
              this.getList()
            }, 500)
          }).catch(error => {
            this.$message.error(error.msg || '停止失败')
          })
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      // 如果没有传递row参数，说明是顶部删除按钮被点击
      if (!row) {
        this.$message.info('请使用卡片上的删除按钮来删除WebSocket组件')
        return
      }

      const ids = row.id || this.ids
      const names = row.name || this.list.filter(item => this.ids.includes(item.id)).map(
        item => item.name).join(',')

      // 验证是否有选中的项目
      if (!ids || (Array.isArray(ids) && ids.length === 0)) {
        this.$message.warning('请先选择要删除的WebSocket组件')
        return
      }

      this.$confirm({
        title: '确认删除',
        content: `确定要删除WebSocket网络组件"${names}"吗？`,
        onOk: () => {
          const api = Array.isArray(ids) ? delNetworkBatch : delNetwork
          const params = Array.isArray(ids) ? ids.join(',') : ids
          api(params).then(response => {
            this.$message.success('删除成功')
            this.getList()
          })
        }
      })
    },
    /** 查看详情 */
    handleView(row) {
      this.$router.push(`/system/network/websocket/detail/${row.id}`)
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$message.info('导出功能待实现')
    },
    getDisplayName(item) {
      // 如果产品名称不为空，显示产品名称，否则显示组件名称
      const productName = this.productName(item)
      return productName || item.name
    },
    productName(item) {
      const found = this.productOptions.find(opt => opt.value === item.productKey)
      return found ? found.label : ''
    },
    getConfigValue(item, key, defaultValue = '未配置') {
      try {
        const config = JSON.parse(item.configuration)
        return config[key] || defaultValue
      } catch (error) {
        return '配置错误'
      }
    },
    /** 显示绑定产品 */
    showBindProducts(item) {
      this.currentNetwork = item
      this.currentBindProducts = item.bindWebSocketProducts || []
      this.bindProductsVisible = true
    },
    /** 检查WebSocket配置是否完整 */
    isWebSocketConfigured(item) {
      if (!item.configuration) return false
      try {
        const config = typeof item.configuration === 'string'
          ? JSON.parse(item.configuration)
          : item.configuration
        // WebSocket 必须配置 port 和 path
        return !!(config.port && config.path)
      } catch (e) {
        return false
      }
    },
    /** 获取状态文本 */
    getStatusText(item) {
      if (item.running) {
        return '运行中'
      } else if (this.isWebSocketConfigured(item)) {
        return '已停止'
      } else {
        return '未配置'
      }
    },
    /** 获取状态样式类 */
    getStatusClass(item) {
      if (item.running) {
        return 'status-running'
      } else if (this.isWebSocketConfigured(item)) {
        return 'status-stopped'
      } else {
        return 'status-unconfigured'
      }
    },
    /** 获取产品图片 */
    getProductImage(item) {
      if (!item.photoUrl) return null

      // 如果是字符串，尝试解析JSON
      if (typeof item.photoUrl === 'string') {
        try {
          const parsed = JSON.parse(item.photoUrl)
          return parsed.img || null
        } catch (e) {
          // 如果不是JSON，直接返回字符串
          return item.photoUrl
        }
      }

      // 如果是对象，直接取img属性
      if (typeof item.photoUrl === 'object') {
        return item.photoUrl.img || null
      }

      return null
    },
    /** 显示绑定产品 */
    showBindProduct(item) {
      if (!item.productKey) return
      
      // 查找对应的产品信息
      const product = this.productOptions.find(p => p.value === item.productKey)
      if (product) {
        this.currentNetwork = item
        this.currentBindProduct = {
          name: product.label,
          productKey: product.value
        }
        this.bindProductVisible = true
      }
    }
  }
}
</script>

<style lang="less" scoped>
.table-page-search-wrapper {
  .table-page-search-submitButtons {
    display: flex;
    align-items: center;
    height: 32px;
  }
}

.table-operations {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .refresh-btn {
    margin-left: auto;
  }
}

.type-cell {
  display: flex;
  align-items: center;
}

.network-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px #f0f1f2;
  transition: box-shadow 0.2s;
  position: relative;
  padding-bottom: 8px;
}

.network-card:hover {
  box-shadow: 0 4px 16px #e6f7ff;
}

.network-card.card-enabled {
  border-left: 4px solid #52c41a;
  background: linear-gradient(135deg, #f6ffed 0%, #ffffff 100%);
}

.network-card.card-disabled {
  border-left: 4px solid #ff4d4f;
  background: linear-gradient(135deg, #fff2f0 0%, #ffffff 100%);
  opacity: 0.8;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-title {
  margin-left: 8px;
  cursor: pointer;
  color: #1890ff;
  transition: color 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  flex: 1;
}

.card-title:hover {
  color: #40a9ff;
}

.card-body {
  margin: 12px 0 8px 0;
}

.card-row {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  justify-content: center;
  align-items: center;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e8e8e8;
  font-size: 16px;
  background: #ffffff;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.action-btn.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.action-btn.disabled:hover {
  transform: none;
  box-shadow: none;
}

.start-btn {
  color: #389e0d;
}

.start-btn:hover:not(.disabled) {
  background: #f6ffed;
  border-color: #b7eb8f;
  color: #52c41a;
}

.stop-btn {
  color: #d46b08;
}

.stop-btn:hover:not(.disabled) {
  background: #fff7e6;
  border-color: #ffd591;
  color: #fa8c16;
}

.edit-btn {
  color: #0958d9;
}

.edit-btn:hover:not(.disabled) {
  background: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.delete-btn {
  color: #cf1322;
}

.delete-btn:hover:not(.disabled) {
  background: #fff2f0;
  border-color: #ffccc7;
  color: #ff4d4f;
}

.product-count-btn {
  padding: 0;
  height: auto;
  color: #1890ff;
  font-size: 13px;
}

.product-count-btn:hover {
  color: #40a9ff;
}

.product-count-inline {
  display: inline;
}

.no-bind-text {
  color: #999;
  font-size: 13px;
}

.status-running {
  color: #52c41a;
  font-weight: 500;
}

.status-stopped {
  color: #fa8c16;
  font-weight: 500;
}

.status-unconfigured {
  color: #999;
  font-weight: 500;
}

.breath-badge {
  animation: breath-scale 1.2s infinite ease-in-out;
}

@keyframes breath-scale {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.5);
  }
  100% {
    transform: scale(1);
  }
}

// WebSocket类型标识样式 - 低调设计
.websocket-type-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
}

.websocket-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  color: #8c8c8c;
  line-height: 1.2;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
</style>
