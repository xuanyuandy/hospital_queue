import request from '@/utils/request'

export function getHospSetList(current, limit, searchObj) {
  return request({
    url: `/admin/hosp/hospitalSet/findPageHospSet/${current}/${limit}`,
    method: 'post',
    data: searchObj // 使用json deliver
  })
}

export function deleteHospSet(id) {
  return request({
    url: `/admin/hosp/hospitalSet/${id}`,
    method: 'delete'
  })
}

export function batchRemoveHospSet(idList) {
  return request({
    url: `/admin/hosp/hospitalSet/batchRemove`,
    method: 'delete',
    data: idList
  })
}

// 锁定和取消锁定
export function lockHospSet(id, status) {
  return request({
    url: `/admin/hosp/hospitalSet/lockHospitalSet/${id}/${status}`,
    method: 'put'
  })
}

export function saveHospSet(hospitalSet) {
  return request({
    url: `/admin/hosp/hospitalSet/saveHospitalSet`,
    method: 'post',
    data: hospitalSet
  })
}

export function getHospSet(id) {
  return request({
    url: `/admin/hosp/hospitalSet/getHospSet/${id}`,
    method: 'get'
  })
}

export function updateHospSet(hospitalSet) {
  return request({
    url: `/admin/hosp/hospitalSet/updateHospitalSet`,
    method: 'post',
    data: hospitalSet
  })
}
