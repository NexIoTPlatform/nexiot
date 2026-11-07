import type { ButtonProps } from '@arco-design/web-vue'

export interface TableAction {
  key: string
  label: string
  icon: 'view' | 'edit' | 'delete' | 'add' | 'lock' | 'safe' | 'users' | 'storage' | 'apply' | 'publish' | 'withdraw' | 'play' | 'stop' | 'history' | 'retry' | 'copy' | 'unlink'
  visible?: boolean
  disabled?: boolean
  status?: ButtonProps['status']
  confirm?: string
  href?: string
  onClick?: () => unknown
}
