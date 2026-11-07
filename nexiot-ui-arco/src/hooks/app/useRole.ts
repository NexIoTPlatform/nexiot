import type * as T from '@/apis/system/role'
import { ref } from 'vue'
import { getRoleOptions } from '@/apis/system/role'

/** 角色模块 */
export function useRole() {
  const loading = ref(false)
  const roleList = ref<T.ListItem[]>([])
  const total = ref(0)
  const getRoleList = async () => {
    try {
      loading.value = true
      const res = await getRoleOptions()
      roleList.value = res.data.filter((i) => i.status === '1')
      total.value = res.data.length
    } finally {
      loading.value = false
    }
  }
  return { roleList, getRoleList, loading, total }
}
