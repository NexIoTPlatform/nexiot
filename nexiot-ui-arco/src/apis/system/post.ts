import http from '@/utils/http'

interface NexIoTPost {
  postId: string | number
  postCode: string
  postName: string
  postSort: number
  status: '0' | '1'
  remark?: string
  createTime?: string
}

export interface PostItem {
  id: string
  code: string
  name: string
  sort: number
  status: Status
  remark: string
  createTime: string
}

function mapPost(item: NexIoTPost): PostItem {
  return {
    id: String(item.postId),
    code: item.postCode,
    name: item.postName,
    sort: item.postSort,
    status: item.status === '0' ? '1' : '0',
    remark: item.remark || '',
    createTime: item.createTime || ''
  }
}

function toPayload(data: Partial<PostItem>) {
  return {
    postId: data.id || undefined,
    postCode: data.code,
    postName: data.name,
    postSort: data.sort,
    status: data.status === '0' ? '1' : '0',
    remark: data.remark
  }
}

export async function getPostList(params: { page: number, size: number, name?: string, code?: string, status?: Status }) {
  const res = await http.get<PageRes<NexIoTPost[]>>('/admin/system/post/list', {
    pageNum: params.page,
    pageSize: params.size,
    postName: params.name,
    postCode: params.code,
    status: params.status === undefined ? undefined : params.status === '1' ? '0' : '1'
  })
  return { ...res, data: { records: res.data.records.map(mapPost), total: res.data.total } }
}

export async function getPost(id: string) {
  const res = await http.get<NexIoTPost>(`/admin/system/post/${id}`)
  return { ...res, data: mapPost(res.data) }
}

export function addPost(data: Partial<PostItem>) {
  return http.post<unknown>('/admin/system/post', toPayload(data))
}

export function updatePost(data: Partial<PostItem>) {
  return http.put<unknown>('/admin/system/post', toPayload(data))
}

export function deletePost(ids: string[]) {
  return http.delete<unknown>(`/admin/system/post/${ids.join(',')}`)
}
