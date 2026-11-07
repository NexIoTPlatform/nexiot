import { Card, Drawer, Modal, Table } from '@arco-design/web-vue'
// 额外引入 Arco Design Icon图标库
import ArcoVueIcon from '@arco-design/web-vue/es/icon'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
// import '@arco-themes/vue-gi-demo/index.less'
// import '@arco-design/web-vue/dist/arco.css'
import { createApp } from 'vue'
// 状态管理
import pinia from '@/stores'
import App from './App.vue'
// 自定义指令
import directives from './directives'
import router from './router'
// 导入 Arco Design 主题和自定义样式
import '@/styles/arco-ui/index.less'
// 使用动画库
import 'animate.css/animate.min.css'
// 自定义过渡动画
import '@/styles/css/transition.css'
// 导入全局scss主文件
import '@/styles/index.scss'
// 注册离线图标集
import '@/icons/index'

// 对特定组件进行默认配置
Card.props.bordered = false
// 所有表格默认显示单元格网格，页面无需单独配置。
Table.props.bordered.default = () => ({ wrapper: true, cell: true })
use([CanvasRenderer, BarChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent])

const app = createApp(App)
Modal._context = app._context
Drawer._context = app._context

app.use(router)
app.use(pinia)
app.use(ArcoVueIcon)
app.use(directives)

app.mount('#app')
