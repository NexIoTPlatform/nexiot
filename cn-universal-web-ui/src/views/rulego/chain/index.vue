<template>
  <div>
    <page-header-wrapper>
      <a-card :bordered="false">
        <!-- 条件搜索 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline">
            <a-row :gutter="48">
              <a-col :md="8" :sm="24">
                <a-form-item label="规则链名称" prop="chainName">
                  <a-input v-model="queryParams.chainName" placeholder="请输入规则链名称"
                           @keyup.enter="handleQuery" allow-clear/>
                </a-form-item>
              </a-col>
              <a-col :md="8" :sm="24">
                <a-form-item :label="$t('common.status')" prop="status">
                  <a-select v-model="queryParams.status" placeholder="请选择状态" style="width: 100%"
                            allow-clear>
                    <a-select-option value="draft">草稿</a-select-option>
                    <a-select-option value="deployed">已部署</a-select-option>
                    <a-select-option value="stopped">已停止</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="8" :sm="24">
                <span class="table-page-search-submitButtons">
                  <a-button type="primary" @click="handleQuery">
                    <a-icon type="search"/>
                    {{ $t('button.search') }} </a-button>
                  <a-button style="margin-left: 8px" @click="resetQuery">
                    <a-icon type="reload"/>
                    重置
                  </a-button>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 操作 -->
        <div class="table-operations">
          <a-button type="primary" @click="handleAdd"
                    v-hasPermi="['rulego:chain:add']">
            <a-icon type="plus"/>
            {{ $t('button.add') }}</a-button>
          <a-button type="primary" size="small" :loading="loading" :style="{ float: 'right' }"
                    @click="getList">
            <a-icon type="sync" :spin="loading"/>
          </a-button>
        </div>

        <!-- 数据展示 -->
        <a-table :loading="loading" :size="tableSize" rowKey="id" :columns="columns"
                 :data-source="chainList"
                 :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
                 :pagination="false">
          <div slot="chainInfo" slot-scope="text, record" class="chain-info-cell">
            <div class="chain-info-content">
              <div class="chain-name">
                <a-button type="link" @click="handleDesign(record)" class="chain-name-link">
                  {{ record.chainName || '未命名规则链' }}
                </a-button>
              </div>
              <div class="chain-id">{{ record.rulegoId }}</div>
            </div>
          </div>

          <div slot="description" slot-scope="text, record" class="description-cell">
            <div class="description-content">
              <span v-if="record.description" class="description-text">{{ record.description }}</span>
              <span v-else class="description-empty">暂无描述</span>
            </div>
          </div>

          <div slot="status" slot-scope="text, record" class="status-cell">
            <div
              :class="{ 'status-badge draft': record.status === 'draft', 'status-badge deployed': record.status === 'deployed', 'status-badge stopped': record.status === 'stopped' }">
              <span class="status-dot"></span>
              <span class="status-text">{{ getStatusText(record.status) }}</span>
            </div>
          </div>

          <div slot="createTime" slot-scope="text, record" class="time-cell">
            <div class="time-main">{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</div>
          </div>

          <div slot="lastSyncTime" slot-scope="text, record" class="time-cell">
            <div class="time-main">{{ parseTime(record.lastSyncTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</div>
          </div>

          <span slot="operation" slot-scope="text, record" class="operation-buttons">
            <a @click="handleDesign(record)" v-hasPermi="['rulego:chain:design']" class="operation-btn">
              <a-icon type="edit"/>
              {{ $t('button.design') }} </a>
            <a-divider type="vertical" v-hasPermi="['rulego:chain:edit']"/>
            <a @click="handleUpdate(record)" v-hasPermi="['rulego:chain:edit']" class="operation-btn">
              <a-icon type="edit"/>
              {{ $t('button.edit') }} </a>
            <a-divider type="vertical" v-if="record.status === 'draft'" v-hasPermi="['rulego:chain:deploy']"/>
            <a @click="handleDeploy(record)" v-if="record.status === 'draft'" v-hasPermi="['rulego:chain:deploy']"
               class="operation-btn">
              <a-icon type="upload"/>
              {{ $t('button.deploy') }} </a>
            <a-divider type="vertical" v-if="record.status === 'deployed'" v-hasPermi="['rulego:chain:stop']"/>
            <a @click="handleStop(record)" v-if="record.status === 'deployed'" v-hasPermi="['rulego:chain:stop']"
               class="operation-btn">
              <a-icon type="pause"/>
              {{ $t('button.stop') }}</a>
            <a-divider type="vertical" v-if="record.status === 'stopped'" v-hasPermi="['rulego:chain:deploy']"/>
            <a @click="handleStart(record)" v-if="record.status === 'stopped'" v-hasPermi="['rulego:chain:deploy']"
               class="operation-btn">
              <a-icon type="caret-right"/>
              {{ $t('button.start') }}</a>
            <a-divider type="vertical" v-hasPermi="['rulego:chain:sync']"/>
            <a @click="handleSync(record)" v-hasPermi="['rulego:chain:sync']" class="operation-btn">
              <a-icon type="reload"/>
              {{ $t('button.sync') }} </a>
            <a-divider type="vertical" v-hasPermi="['rulego:chain:remove']"/>
            <a style="color:#F53F3F" @click="handleDelete(record)" v-hasPermi="['rulego:chain:remove']"
               class="operation-btn">
              <a-icon type="delete"/>
              {{ $t('button.delete') }} </a>
          </span>
        </a-table>

        <!-- 分页 -->
        <a-pagination class="ant-table-pagination" show-size-changer show-quick-jumper
                      :current="queryParams.pageNum"
                      :total="total" :page-size="queryParams.pageSize"
                      :showTotal="total => `共 ${total} 条`"
                      @showSizeChange="onShowSizeChange" @change="changeSize"/>
      </a-card>
    </page-header-wrapper>

    <!-- 添加或修改规则链对话框 -->
    <a-modal :title="title" :visible="open" @ok="submitForm" @cancel="cancel" width="500px">
      <a-form-model ref="form" :model="form" :rules="rules">
        <a-form-model-item label="规则链名称" prop="chainName">
          <a-input v-model="form.chainName" placeholder="请输入规则链名称"/>
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-textarea v-model="form.description" placeholder="请输入描述" :rows="4"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <!-- rulego设计器对话框 -->
    <a-modal
      title="规则链设计器"
      :visible="designerOpen"
      @cancel="handleDesignerClose"
      width="95%"
      :footer="null"
      :centered="true"
      :destroyOnClose="true"
      :maskClosable="false"
    >
      <div :style="{ height: isFullscreen ? '100vh' : '80vh', position: 'relative' }">
        <!-- 全屏按钮 -->
        <!-- <div style="position: absolute; top: 10px; right: 10px; z-index: 1000;">
          <a-button 
            type="primary" 
            size="small" 
            @click="toggleFullscreen"
            :icon="isFullscreen ? 'fullscreen-exit' : 'fullscreen'"
          >
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </a-button>
        </div> -->

        <iframe
          :src="designerUrl"
          width="100%"
          height="100%"
          frameborder="0"
          ref="designerFrame"
          :style="{ 
            height: '100%',
            position: isFullscreen ? 'fixed' : 'relative',
            top: isFullscreen ? '0' : 'auto',
            left: isFullscreen ? '0' : 'auto',
            zIndex: isFullscreen ? '9999' : 'auto',
            width: isFullscreen ? '100vw' : '100%'
          }"
        ></iframe>
      </div>
    </a-modal>
  </div>
</template>

<script>
import {
  addChain,
  delChain,
  deployChain,
  getChain,
  getDesignerUrl,
  listChain,
  stopChain,
  syncChain,
  updateChain
} from "@/api/rulego/chain";

export default {
  name: "RulegoChain",

  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      selectedRowKeys: [],
      selectedRows: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 表格大小
      tableSize: 'middle',
      // 总条数
      total: 0,
      // 规则链表格数据
      chainList: [],
      // 表格列定义
      columns: [
        {
          title: this.$t('rule.ruleChainId'),
          dataIndex: 'chainInfo',
          scopedSlots: {customRender: 'chainInfo'},
          width: '20%',
          align: 'left'
        },
        {
          title: this.$t('rule.description'),
          dataIndex: 'description',
          scopedSlots: {customRender: 'description'},
          width: '20%',
          align: 'left'
        },
        {
          title: this.$t('common.status'),
          dataIndex: 'status',
          scopedSlots: {customRender: 'status'},
          width: '10%',
          align: 'center'
        },
        {
          title: this.$t('time.create'),
          dataIndex: 'createTime',
          scopedSlots: {customRender: 'createTime'},
          width: '15%',
          align: 'center'
        },
        {
          title: this.$t('rule.lastSyncTime'),
          dataIndex: 'lastSyncTime',
          scopedSlots: {customRender: 'lastSyncTime'},
          width: '15%',
          align: 'center'
        },
        {
          title: this.$t('user.operation'),
          dataIndex: 'operation',
          width: '20%',
          scopedSlots: {customRender: 'operation'},
          align: 'center'
        }
      ],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示设计器弹出层
      designerOpen: false,
      // 设计器URL
      designerUrl: "",
      // 是否全屏
      isFullscreen: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        chainName: null,
        status: null
      },
      // 表单参数
      form: {
        id: null,
        chainName: null,
        description: null
      },
      // 表单校验
      rules: {
        chainName: [
          {required: true, message: "规则链名称不能为空", trigger: "blur"}
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询规则链列表 */
    getList() {
      this.loading = true;
      listChain(this.queryParams).then(response => {
        this.chainList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        chainName: null,
        description: null
      };
      this.$nextTick(() => {
        if (this.$refs.form && this.$refs.form.resetFields) {
          this.$refs.form.resetFields();
        }
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        chainName: null,
        status: null
      };
      this.$nextTick(() => {
        if (this.$refs.queryForm && this.$refs.queryForm.resetFields) {
          this.$refs.queryForm.resetFields();
        }
      });
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加规则链";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getChain(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改规则链";
      });
    },
    /** 设计按钮操作 */
    handleDesign(row) {
      getDesignerUrl(row.id).then(response => {
        this.designerUrl = response.data;
        this.designerOpen = true;
        this.isFullscreen = false; // 重置全屏状态
      });
    },
    /** 部署按钮操作 */
    handleDeploy(row) {
      this.$confirm({
        title: '确认部署规则链?',
        content: '是否确认部署规则链"' + row.chainName + '"？',
        onOk: () => {
          return deployChain(row.id).then(() => {
            this.getList();
            this.$message.success("部署成功");
          });
        }
      });
    },
    /** 停止按钮操作 */
    handleStop(row) {
      this.$confirm({
        title: '确认停止规则链?',
        content: '是否确认停止规则链"' + row.chainName + '"？',
        onOk: () => {
          return stopChain(row.id).then(() => {
            this.getList();
            this.$message.success("停止成功");
          });
        }
      });
    },
    /** 开始按钮操作（规则链停止时调用部署API重新启动） */
    handleStart(row) {
      this.$confirm({
        title: '确认启动规则链?',
        content: '是否确认启动规则链"' + row.chainName + '"？',
        onOk: () => {
          return deployChain(row.id).then(() => {
            this.getList();
            this.$message.success("启动成功");
          });
        }
      });
    },
    /** 同步按钮操作 */
    handleSync(row) {
      this.$confirm({
        title: '确认同步规则链?',
        content: '是否确认同步规则链"' + row.chainName + '"的DSL？',
        onOk: () => {
          return syncChain(row.id).then(() => {
            this.getList();
            this.$message.success("同步成功");
          });
        }
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$nextTick(() => {
        if (this.$refs.form && this.$refs.form.validate) {
          this.$refs.form.validate(valid => {
            if (valid) {
              if (this.form.id != null) {
                updateChain(this.form).then(response => {
                  this.$message.success("修改成功");
                  this.open = false;
                  this.getList();
                });
              } else {
                addChain(this.form).then(response => {
                  this.$message.success("新增成功");
                  this.open = false;
                  this.getList();
                });
              }
            } else {
              return false;
            }
          });
        } else {
          console.error('Form reference not found or validate method not available');
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$confirm({
        title: '确认删除规则链?',
        content: '是否确认删除规则链编号为"' + ids + '"的数据项？',
        onOk: () => {
          return delChain(ids).then(() => {
            this.getList();
            this.$message.success("删除成功");
          });
        }
      });
    },
    /** 设计器关闭处理 */
    handleDesignerClose() {
      this.designerOpen = false;
      this.designerUrl = "";
      this.isFullscreen = false;
      // 关闭设计器后刷新列表，以获取最新的DSL内容
      this.getList();
    },
    /** 切换全屏 */
    toggleFullscreen() {
      this.isFullscreen = !this.isFullscreen;
    },
    /** 分页大小改变 */
    onShowSizeChange(current, pageSize) {
      this.queryParams.pageSize = pageSize;
      this.getList();
    },
    /** 分页改变 */
    changeSize(current, pageSize) {
      this.queryParams.pageNum = current;
      this.queryParams.pageSize = pageSize;
      this.getList();
    },
    /** 多选框选中数据 */
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
      this.ids = this.selectedRows.map(item => item.id);
      this.single = selectedRowKeys.length !== 1;
      this.multiple = !selectedRowKeys.length;
    },
    /** 获取状态文本 */
    getStatusText(status) {
      const statusMap = {
        'draft': '草稿',
        'deployed': '已部署',
        'stopped': '已停止'
      };
      return statusMap[status] || status;
    },
    /** 获取状态颜色 */
    getStatusColor(status) {
      const colorMap = {
        'draft': 'default',
        'deployed': 'green',
        'stopped': 'orange'
      };
      return colorMap[status] || 'default';
    },
    /** 格式化时间 */
    parseTime(val, format) {
      if (!val) {
        return '';
      }
      const d = new Date(val);
      if (isNaN(d.getTime())) {
        return val;
      }
      if (format) {
        const pad = n => n < 10 ? '0' + n : n;
        return format
          .replace('{y}', d.getFullYear())
          .replace('{m}', pad(d.getMonth() + 1))
          .replace('{d}', pad(d.getDate()))
          .replace('{h}', pad(d.getHours()))
          .replace('{i}', pad(d.getMinutes()))
          .replace('{s}', pad(d.getSeconds()));
      }
      const pad = n => n < 10 ? '0' + n : n;
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
        d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }
  }
};
</script>

<style scoped lang="less">
.chain-info-cell {
  .chain-info-content {
    .chain-name {
      font-weight: 500;
      margin-bottom: 4px;

      .chain-name-link {
        padding: 0;
        height: auto;
        font-size: 14px;
        color: #1890ff;

        &:hover {
          color: #40a9ff;
        }
      }
    }

    .chain-id {
      font-size: 12px;
      color: #666;
      margin-bottom: 2px;
    }

    .chain-desc {
      font-size: 12px;
      color: #999;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.status-cell {
  .status-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      margin-right: 4px;
    }

    &.draft {
      background-color: #f0f0f0;
      color: #666;

      .status-dot {
        background-color: #666;
      }
    }

    &.deployed {
      background-color: #f6ffed;
      color: #52c41a;

      .status-dot {
        background-color: #52c41a;
      }
    }

    &.stopped {
      background-color: #fff2e8;
      color: #fa8c16;

      .status-dot {
        background-color: #fa8c16;
      }
    }
  }
}

.time-cell {
  .time-main {
    font-size: 14px;
    color: #333;
  }
}

.operation-buttons {
  .operation-btn {
    color: #1890ff;
    text-decoration: none;
    margin-right: 8px;

    &:hover {
      color: #40a9ff;
    }

    &:last-child {
      margin-right: 0;
    }
  }
}
</style>
