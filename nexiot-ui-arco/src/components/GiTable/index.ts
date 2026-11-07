import type { TableInstance as ArcoTableInstance } from '@arco-design/web-vue'
import Table from './GiTable.vue'

export type * from './type'
export interface TableInstance {
  tableRef?: ArcoTableInstance
}
export { Table as GiTable }
