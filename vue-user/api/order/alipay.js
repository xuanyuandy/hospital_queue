import request from '@/utils/request'

const api_name = `/api/order/alipay`

export default {
  createNative(orderId) {
    return request({
      url: `${api_name}/createNative/${orderId}`,
      method: 'get'
    })
  },
  queryPayStatus(orderId) {
    return request({
      url: `${api_name}/queryPayStatus/${orderId}`,
      method: 'get'
    })
  }
}
