import request from '@/utils/request'

const api_name = `/get`

export default {
    getBegin(name){
        return request({
            url: `${api_name}/begin/${name}`,
            method: 'get'
          })
    },
    getMid(name,arrive){
        return request({
            url: `${api_name}/mid/${name}/${arrive}`,
            method: 'get'
          })
    },
    getEnd(name){
        return request({
            url: `${api_name}/end/${name}`,
            method: 'get'
          })
    },
    getWhole(){
        return request({
            url: `${api_name}/whole`,
            method: 'get' 
        })
    },
    getModify(){
        return request({
            url: `${api_name}/modify`,
            method: 'get' 
        })
    },
    getNow(){
        return request({
            url: `${api_name}/now`,
            method: 'get' 
        })
    },
    getSpecial(name){
        return request({
            url: `${api_name}/special/${name}`,
            method: 'get' 
        })
    },
    getSpecialSize(name){
        return request({
            url: `${api_name}/special/size/${name}`,
            method: 'get' 
        })
    }

}