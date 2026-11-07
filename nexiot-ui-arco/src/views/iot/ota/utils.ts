interface OtaTransferState {
  phase?: string
  ackedChunks?: number
  totalChunks?: number
  retries?: number
  profile?: { mode?: string }
}

export function describeOtaTransfer(value?: string, status?: string) {
  let state: OtaTransferState = {}
  try {
    const parsed: unknown = JSON.parse(value || '{}')
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) state = parsed as OtaTransferState
  } catch { /* 历史任务没有分片状态。 */ }
  const total = Number(state.totalChunks || 0)
  const acked = Math.min(total, Math.max(0, Number(state.ackedChunks || 0)))
  const phaseLabels: Record<string, string> = {
    START: '等待设备开始确认',
    CHUNK: '等待分片确认',
    WAIT_REQUEST: '等待设备索包',
    FINISH: '文件收齐，等待设备安装',
    COMPLETE: '传输完成，等待升级结果'
  }
  return {
    chunks: total > 0 ? `${acked} / ${total} 包已确认` : state.profile?.mode === 'URL' ? '整包下载' : '-',
    phase: status === 'SUCCESS' ? '升级成功' : status === 'FAILED' ? '升级失败，人工重试将重新传输' : status === 'CANCELED' ? '已取消' : phaseLabels[state.phase || ''] || '',
    retries: Math.max(0, Number(state.retries || 0))
  }
}

export const otaDeviceStatusLabels: Record<string, string> = {
  WAITING_PUSH: '等待下发',
  PUSHED: '已下发',
  DOWNLOADING: '传输中',
  VERIFYING: '文件校验中',
  INSTALLING: '安装中',
  REBOOTING: '重启中',
  WAITING_VERSION: '等待版本确认',
  SUCCESS: '成功',
  FAILED: '失败',
  TIMEOUT: '已超时',
  SKIPPED: '已跳过',
  CANCELED: '已取消',
  PENDING: '等待中'
}
