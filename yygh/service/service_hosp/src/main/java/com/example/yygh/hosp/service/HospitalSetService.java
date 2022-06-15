package com.example.yygh.hosp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yygh.model.hosp.HospitalSet;
import com.example.yygh.vo.order.SignInfoVo;

public interface HospitalSetService extends IService<HospitalSet> {
    String getSignKey(String hoscode);

    //获取医院签名信息
    SignInfoVo getSignInfoVo(String hoscode);
}
