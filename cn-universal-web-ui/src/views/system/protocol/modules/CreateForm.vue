<template>
  <a-drawer
    width="700px"
    :label-col="4"
    :wrapper-col="14"
    :visible="open"
    @close="onClose"
    @after-visible-change="handleDrawerVisibleChange"
    :keyboard="!editorFullscreen"
    :mask-closable="!editorFullscreen"
  >
    <a-spin :spinning="submitLoading" tip="正在保存，请稍候..." size="large">
      <a-divider orientation="left">
        <b>{{ formTitle }}</b>
      </a-divider>
      <a-form-model ref="form" :model="form" :rules="rules">
        <a-row :gutter="8">
          <a-col :span="12">
            <a-form-model-item :label="$t('product.name')" prop="id">
              <a-select
                v-model="form.id"
                show-search
                placeholder="请选择或搜索产品名称"
                style="width: 100%"
                :filter-option="false"
                not-found-content="无可配置的产品"
                @search="handleSearch"
                @change="handleChange"
                @focus="handleFocus"
                :disabled="formType===2 || productType || submitLoading"
              >
                <a-select-option
                  v-for="item in data"
                  :key="item.value"
                  :value="item.value"
                  :title="item.label"
                >
                  <div style="display: flex; flex-direction: column;">
                    <span style="font-weight: 500;">{{ item.label }}</span>
                    <span style="font-size: 12px; color: #b0b3b9;">ProductKey: {{
                        item.value
                      }}</span>
                  </div>
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="8">
          <a-col :span="24">
            <a-form-model-item label="驱动名称" prop="name">
              <a-input v-model="form.name" style="width:100%" placeholder="请输入驱动名称"
                       :disabled="submitLoading"/>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="8">
          <a-col :span="12">
            <a-form-model-item label="版本号" prop="version">
              <a-input v-model="form.version" placeholder="请输入版本号，如：1.0.0" :disabled="submitLoading"/>
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="发布状态" prop="state">
              <a-switch
                :checked="form.state == 1"
                checked-children="已发布"
                un-checked-children="未发布"
                @change="val => form.state = val ? 1 : 0"
                style="vertical-align: middle"
                :disabled="submitLoading"
              />
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="8">
          <a-col :span="24">
            <a-form-model-item label="类型" prop="type">
              <a-select
                placeholder="请选择类型"
                @change="changeType"
                style="width: 100%"
                v-model="form.type"
                allow-clear
                :disabled="submitLoading">
                <a-select-option v-for="(d, index) in protocolTypeOptions" :key="index"
                                 :value="d.dictValue">
                  <span class="type-cell">
                    <template v-if="d.dictValue === 'jar'">
                      <svg class="type-icon" viewBox="0 0 32 32"><g><path fill="#5382a1"
                                                                          d="M23.47 27.19s.59.48-.65.85c-2.36.72-9.77.94-11.84.03-.74-.32.66-.76 1.1-.85.46-.1.72-.08.72-.08-.83-.58-5.37 1.15-2.3 1.65 8.36 1.36 15.23-.61 13.98-1.6z"/><path
                        fill="#e76f00"
                        d="M12.87 19.7s-3.81.91-1.35 1.24c1.04.14 3.12.11 5.06-.06 1.59-.14 3.18-.44 3.18-.44s-.56.24-.97.52c-3.91 1.03-11.47.55-9.29-.5 1.84-.87 3.37-.76 3.37-.76z"/><path
                        fill="#5382a1"
                        d="M21.06 22.13c3.97-2.06 2.14-4.04.86-3.77-.32.07-.46.13-.46.13s.12-.19.36-.27c2.66-.94 4.71 2.76-.86 4.01z"/><path
                        fill="#e76f00"
                        d="M13.32 24.13c-2.1-.47-4.33-.36-4.33-.36s.44.36 1.32.65c3.98 1.2 12.09.32 9.78-.62-1.93-.77-3.77-.67-6.77-.33z"/><path
                        fill="#5382a1"
                        d="M25.5 29.5c-3.98.76-9.02.67-11.97.18-.6-.1-.73-.16-.73-.16s.09.07.26.15c2.01.88 8.36 1.17 13.23.34.93-.15.29-.51-.79-.51z"/><path
                        fill="#e76f00"
                        d="M19.5 15.5c1.5-1.5 2.5-3.5 1.5-5.5-1-2-3.5-2.5-5-1.5-1.5 1-2 3.5-1 5.5 1 2 3.5 2.5 5 1.5z"/></g></svg>
                      <span>Java</span>
                    </template>
                    <template v-else-if="d.dictValue === 'jscript'">
                      <svg class="type-icon" viewBox="0 0 32 32">
                        <rect width="32" height="32" rx="6" fill="#fff"/>
                        <text
                          x="7"
                          y="24"
                          font-size="16"
                          font-family="Arial"
                          font-weight="bold"
                          fill="#f7df1e"
                          stroke="#222"
                          stroke-width="1">JS</text>
                      </svg>
                      <span>JavaScript</span>
                    </template>
                    <template v-else-if="d.dictValue === 'magic'">
                      <svg class="type-icon" viewBox="0 0 32 32" width="18" height="18"><circle
                        cx="16" cy="16" r="14" fill="#722ed1"/><path d="M16 8v8l6 3" stroke="#fff"
                                                                     stroke-width="2"
                                                                     stroke-linecap="round"
                                                                     fill="none"/><circle cx="16"
                                                                                          cy="16"
                                                                                          r="2"
                                                                                          fill="#fff"/></svg>
                      <span>Magic</span>
                    </template>
                    <template v-else>
                      <span>{{ d.dictLabel }}</span>
                    </template>
                  </span>
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="12">
            <a-form-model-item label="包名" prop="fileName" v-if="type==='jar'">
              <a-input v-model="form.fileName" style="width:70%"
                       :disabled="submitLoading"></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="文件地址" prop="url" v-if="type==='jar'">
              <a-input v-model="form.url" style="width:70%" :disabled="true"></a-input>
              <file-upload @fileList="fileList" type="file" style="display: inline"
                           :disabled="submitLoading"></file-upload>
              <a-button size="small" type="primary" @click="download"
                        v-show="form.url && (form.url.startsWith('http://') || form.url.startsWith('https://'))"
                        :disabled="submitLoading">
                下载文件
              </a-button>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-form-model-item label="插件源码" prop="jscript"
                           v-if="type==='jscript'||type==='magic-script'">
          <div class="editor-toolbar">
            <a-button
              size="small"
              @click="toggleEditorFullscreen(true)"
              :disabled="submitLoading"
            >全屏</a-button>
          </div>
          <div class="editor-adaptive">
            <CodeEditor
              v-model="form.jscript"
              :options="{mode: 'javascript', theme: 'material'}"
              style="width:100%;height:100%;"
              @input="onCodeEditorInput"
              @change="onCodeEditorChange"
              @blur="onCodeEditorBlur"
              @focus="onCodeEditorFocus"
            />
          </div>
          <!-- 全屏覆盖层 -->
          <div v-if="editorFullscreen" class="fullscreen-overlay">
            <div class="fullscreen-toolbar">
              <a-space>
                <a-button type="primary" @click="toggleEditorFullscreen(false)">退出全屏</a-button>
              </a-space>
            </div>
            <div class="fullscreen-editor">
              <CodeEditor
                v-model="form.jscript"
                :options="{mode: 'javascript', theme: 'material'}"
                style="width:100%;height:100%;"
                @input="onCodeEditorInput"
                @change="onCodeEditorChange"
                @blur="onCodeEditorBlur"
                @focus="onCodeEditorFocus"
              />
            </div>
          </div>
        </a-form-model-item>
        <!-- <a-tabs v-model="activeKey" v-if="type">
          <a-tab-pane key="1" tab="解码测试用例">
            <a-tooltip :title="form.example.decode">
              <div class="desc-ellipsis" style="height: 200px;width:700px">
                <a-textarea v-model="form.example.decode" :rows="6" placeholder="请输入解码测试的报文" :disabled="submitLoading">
                </a-textarea>
              </div>
            </a-tooltip>
          </a-tab-pane>
          <a-tab-pane key="2" tab="编码测试用例">
            <a-tooltip :title="form.example.encode">
              <div class="desc-ellipsis" style="height: 200px;width:700px">
                <a-textarea v-model="form.example.encode" :rows="6" placeholder="请输入编码测试的报文" :disabled="submitLoading">
                </a-textarea>
              </div>
            </a-tooltip>
          </a-tab-pane>
        </a-tabs> -->
        <a-form-model-item label="描述" prop="description">
          <a-textarea
            v-model="form.description"
            placeholder="请输入内容"
            :rows="4"
            allow-clear
            :disabled="submitLoading"/>
        </a-form-model-item>
        <div class="bottom-control">
          <a-space>
            <a-button type="primary" @click="submitForm" :loading="submitLoading">
              保存
            </a-button>
            <a-button type="dashed" @click="cancel" :disabled="submitLoading">
              取消
            </a-button>
          </a-space>
        </div>
      </a-form-model>
    </a-spin>
  </a-drawer>
</template>

<script>
import { addProtocol, getProtocol, listProductsWithoutProtocol, messageCodec, updateProtocol } from '@/api/system/protocol';
import CodeEditor from '@/components/CodeEditor.vue';
import FileUpload from '@/components/FileUpload';

export default {
  name: 'CreateForm',
  props: {
    productKey: {
      type: String,
      default: ''
    },
    productNames: {
      type: String,
      default: ''
    },
    protocolTypeOptions: {
      type: Array,
      required: true
    },
    protocolStateOptions: {
      type: Array,
      required: true
    },
    productOption: {
      type: Array,
      required: true
    },
    connectionOptions: {
      type: Array,
      required: true
    }
  },
  components: {
    FileUpload,
    CodeEditor
  },
  data() {
    return {
      decode: true,
      payload: undefined,
      productName: undefined,
      activeKey: '1',
      type: undefined,
      buttonLoading: false,
      loading: false,
      formTitle: '',
      productOptions: [],
      data: [],
      parentUnionId: this.$store.state.user.parentUnionId,
      searchTimer: null, // 搜索防抖定时器
      form: {
        id: undefined,
        payload: undefined,
        name: null,
        description: null,
        state: 0,
        type: undefined,
        configuration: null,
        example: {
          decode: undefined,
          encode: undefined
        },
        version: '',
        fileName: undefined,
        url: undefined,
        jscript: '',
        connectionProtocol: undefined,
        protocolMark: undefined,
        needBs4Decode: false
      },
      needBs4Options: [
        {label: '否', value: 'false'},
        {label: '是', value: 'true'}
      ],
      options: {
        enableBasicAutocompletion: true,
        enableSnippets: true,
        enableLiveAutocompletion: true,
        showFoldWidgets: true,
        fadeFoldWidgets: true
      },
      formType: 1,
      productType: false,
      open: false,
      rules: {
        id: [{required: true, message: 'productKey不能为空', trigger: 'blur'}],
        name: [{required: true, message: '驱动名不能为空', trigger: 'blur'}],
        type: [{required: true, message: '类型不能为空', trigger: 'blur'}],
        fileName: [{required: true, message: '包名不能为空', trigger: 'blur'}],
        jscript: [{required: true, message: '编解码脚本内容不能为空', trigger: 'blur'}],
        url: [{required: true, message: 'jar包文件不能为空', trigger: 'blur'}]
      },
      submitLoading: false
      ,editorFullscreen: false
    }
  },
  filters: {},
  created() {
    this.filterProduct()
  },
  computed: {},
  watch: {
    productOptions() {
      this.data = this.productOptions.map(i => {
        return {label: i.label || i.name, value: i.value || i.productKey}
      })
    },
    'form.type': {
      handler(newName, oldName) {
        console.log('【CreateForm】form.type changed from', oldName, 'to', newName)
        console.log('【CreateForm】formType:', this.formType)
        console.log('【CreateForm】form.jscript before type change:', this.form.jscript)

        if (this.formType === 1 && newName === 'jscript' && !this.form.jscript) {
          console.log('【CreateForm】Setting default jscript template')
          this.form.jscript =
            `//解码
function decode(rawData) {
        var propertiesObj = {
            "messageType": "PROPERTIES",
            "properties": {}
        }
        var eventObj = {
            "messageType": "EVENT",
            "event": "",
            "data": {}
        }

        return JSON.stringify(propertiesObj);

    }
    //编码
    function encode(rawData) {
        return null
}`
          console.log('【CreateForm】form.jscript after setting default jscript:', this.form.jscript)
        }
        if (this.formType === 1 && newName === 'magic' && !this.form.jscript) {
          console.log('【CreateForm】Setting default magic template')
          this.form.jscript =
            `//编码
var encode = payload => {
	return '';
}
//解码
var decode = payload => {
	var propertiesObj = {
		"messageType": "PROPERTIES",
		"properties": {}
	}
	var eventObj = {
		"messageType": "EVENT",
		"event": "",
		"data": {}
	}

	return propertiesObj
}`
          console.log('【CreateForm】form.jscript after setting default magic:', this.form.jscript)
        }
        this.$nextTick(() => {
          if (this.open && (this.type === 'jscript' || this.type === 'magic-script')) {
            window.dispatchEvent(new Event('resize'))
          }
        })
      },
      deep: true
    },
    open(val) {
      if (val && (this.type === 'jscript' || this.type === 'magic-script')) {
        this.$nextTick(() => {
          window.dispatchEvent(new Event('resize'))
        })
      }
    }
  },
  mounted() {
  },
  methods: {
    // 全屏下拦截 ESC（退出全屏而不是关闭抽屉）
    onFullscreenKeydown(e) {
      if (e && (e.key === 'Escape' || e.keyCode === 27)) {
        e.preventDefault()
        e.stopPropagation()
        this.toggleEditorFullscreen(false)
      }
    },
    toggleEditorFullscreen(val) {
      this.editorFullscreen = val
      this.$nextTick(() => {
        // 触发编辑器重绘以适配容器大小变化
        window.dispatchEvent(new Event('resize'))
        // 管理全屏状态下的滚动与键盘监听
        document.body.style.overflow = val ? 'hidden' : ''
        if (val) {
          window.addEventListener('keydown', this.onFullscreenKeydown, true)
        } else {
          window.removeEventListener('keydown', this.onFullscreenKeydown, true)
        }
      })
    },
    messageCodec() {
      messageCodec({id: this.form.id, payload: this.payload, decode: this.decode}).then(
        response => {
          console.log(response)
        })
    },
    changeType(val) {
      if (val === 'jar') {
        this.type = 'jar'
      } else if (val === 'jscript') {
        this.type = 'jscript'
      } else {
        this.type = 'magic-script'
      }
    },
    fileList(val) {
      this.form.url = val.url
    },
    onClose() {
      if (this.submitLoading) {
        this.$message.warning('正在保存中，请稍候...')
        return
      }
      // 关闭抽屉时复位全屏与监听
      if (this.editorFullscreen) {
        this.editorFullscreen = false
        document.body.style.overflow = ''
        window.removeEventListener('keydown', this.onFullscreenKeydown, true)
      }
      this.reset()
      this.open = false
    },
    filterProduct(searchKey) {
      // 使用新的后端接口，直接查询未创建协议且归属人为当前用户的产品
      listProductsWithoutProtocol(searchKey).then((response) => {
        if (response.code === 0 && response.data) {
          // 将产品列表转换为选项格式
          this.productOptions = response.data.map((i) => {
            return {label: i.name, value: i.productKey}
          })
          // 更新显示的数据（不触发新的请求）
          this.updateDisplayData(searchKey || '')
        } else {
          this.productOptions = []
          this.data = []
          if (response.msg) {
            this.$message.warning(response.msg)
          }
        }
      }).catch(err => {
        console.error('获取产品列表失败:', err)
        this.$message.error('获取产品列表失败')
        this.productOptions = []
        this.data = []
      })
    },
    updateDisplayData(searchKey) {
      // 只更新显示数据，不触发后端请求
      this.data = []
      this.productOptions.forEach(i => {
        if (
          searchKey === '' ||
          searchKey === undefined ||
          (i.label && i.label.toLowerCase().indexOf(searchKey.toLowerCase()) > -1) ||
          (i.value && i.value.toLowerCase().indexOf(searchKey.toLowerCase()) > -1)
        ) {
          this.data.push({value: i.value || i.productKey, label: i.label || i.name})
        }
      })
    },
    cancel() {
      if (this.submitLoading) {
        this.$message.warning('正在保存中，请稍候...')
        return
      }
      this.open = false
      this.reset()
    },
    download() {
      window.open(this.form.url)
    },
    reset() {
      // 清除搜索定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.productType = false
      this.formType = 1
      this.type = undefined
      this.productName = undefined
      this.data = []
      this.submitLoading = false
      this.form = {
        id: undefined,
        name: null,
        description: null,
        state: 0,
        type: undefined,
        configuration: null,
        example: {
          decode: undefined,
          encode: undefined
        },
        version: '',
        fileName: undefined,
        url: undefined,
        jscript: '',
        payload: undefined,
        connectionProtocol: undefined,
        protocolMark: undefined
      }
    },
    handleSearch(val) {
      // 清除之前的定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      // 防抖处理：用户停止输入 500ms 后才请求
      this.searchTimer = setTimeout(() => {
        const searchKey = val || ''
        this.filterProduct(searchKey)
      }, 500)
    },
    handleFocus() {
      // 聚焦时，如果产品列表为空，才加载
      if (this.productOptions.length === 0) {
        this.filterProduct('')
      }
    },
    handleChange(val) {
      this.form.id = val
      this.productOptions.forEach(i => {
        if (i.value === val) {
          this.productName = i.label
        }
      })
      // 选择后不需要重新搜索
    },
    handleAdd(row) {
      this.reset()
      this.formType = 1
      // 打开抽屉时重新加载产品列表，确保包含最新创建的产品
      this.filterProduct()
      this.open = true
      this.formTitle = '添加'
    },
    handleUpdate(row, ids) {
      this.reset()
      this.formType = 2
      const id = row.id
      getProtocol(id).then(response => {
        this.form = response.data
        this.form.state = Number(this.form.state)
        if (response.data.example) {
          this.form.example = JSON.parse(response.data.example)
        } else {
          this.form.example = {
            decode: undefined,
            encode: undefined
          }
        }
        this.handleChange(this.form.id)
        this.changeType(this.form.type)
        this.open = true
        this.formTitle = '修改'
      })
    },
    productAdd() {
      this.reset()
      this.productType = true
      this.open = true
      this.formTitle = '添加'
      this.form.id = this.productKey
      this.productName = this.productNames
    },
    submitForm: function () {
      this.$refs.form.validate(valid => {
        if (valid) {
          console.log('【提交时jscript内容】', this.form.jscript)
          console.log('【提交时完整form对象】', JSON.stringify(this.form, null, 2))

          this.form.example = JSON.stringify(this.form.example)
          this.form.state = Number(this.form.state)

          console.log('【最终提交到服务器的数据】', JSON.stringify(this.form, null, 2))

          this.submitLoading = true

          if (this.formType === 2) {
            updateProtocol(this.form).then(response => {
              this.$message.success('修改成功', 3)
              this.open = false
              this.$emit('ok')
            }).finally(() => {
              this.submitLoading = false
            })
          } else {
            addProtocol(this.form).then(response => {
              this.$message.success('新增成功', 3)
              this.open = false
              this.$emit('ok')
            }).catch(error => {
              // 处理错误响应，优先显示服务器返回的错误信息
              let errorMsg = '新增失败'
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
        } else {
          return false
        }
      })
    },
    handleDrawerVisibleChange(val) {
      if (val) {
        // 抽屉打开时，如果是添加模式，重新加载产品列表
        if (this.formType === 1 && !this.productType) {
          this.filterProduct('')
        }
        if (this.type === 'jscript' || this.type === 'magic-script') {
          this.$nextTick(() => {
            window.dispatchEvent(new Event('resize'))
          })
        }
      } else {
        // 抽屉关闭时，清除定时器
        if (this.searchTimer) {
          clearTimeout(this.searchTimer)
          this.searchTimer = null
        }
        // 抽屉隐藏时复位全屏与监听
        if (this.editorFullscreen) {
          this.editorFullscreen = false
          document.body.style.overflow = ''
          window.removeEventListener('keydown', this.onFullscreenKeydown, true)
        }
      }
    },
    onCodeEditorInput(value) {
      console.log('【CreateForm】CodeEditor input event:', value)
      console.log('【CreateForm】form.jscript after input:', this.form.jscript)
    },
    onCodeEditorChange(value) {
      console.log('【CreateForm】CodeEditor change event:', value)
      console.log('【CreateForm】form.jscript after change:', this.form.jscript)
    },
    onCodeEditorBlur(value) {
      console.log('【CreateForm】CodeEditor blur event:', value)
      console.log('【CreateForm】form.jscript after blur:', this.form.jscript)
    },
    onCodeEditorFocus(value) {
      console.log('【CreateForm】CodeEditor focus event:', value)
      console.log('【CreateForm】form.jscript after focus:', this.form.jscript)
    }
  }
}
</script>

<style scoped>
.type-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.type-icon {
  width: 18px;
  height: 18px;
  margin-right: 4px;
  vertical-align: middle;
  display: inline-block;
}

.desc-ellipsis {
  max-width: 350px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  display: inline-block;
}

.editor-adaptive {
  width: 100%;
  height: 300px;
  position: relative;
}

.editor-adaptive > * {
  width: 100% !important;
  height: 100% !important;
}

.editor-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 6px;
}

/* 全屏覆盖层样式 */
.fullscreen-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #fff;
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

.fullscreen-toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.fullscreen-editor {
  flex: 1;
  min-height: 0; /* 保证子元素可正确撑满剩余空间 */
}

/* 加载状态下的样式增强 */
:deep(.ant-spin-spinning) {
  .ant-form-model-item-control {
    opacity: 0.6;
  }

  .ant-input:disabled,
  .ant-select:disabled,
  .ant-switch:disabled {
    opacity: 0.8;
  }
}

.bottom-control {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
