import request from '@/utils/request'

export function dictList(id) {
  return request({
    url: `/admin/cmn/dict/findChildData/${id}`,
    method: 'get'
  })
}

