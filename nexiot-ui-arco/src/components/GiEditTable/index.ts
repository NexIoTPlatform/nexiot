import type { FormInstance } from '@arco-design/web-vue'
import EditTable from './GiEditTable.vue'

export type * from './type'
export interface EditTableInstance {
  formRef?: FormInstance
}
export { EditTable as GiEditTable }
