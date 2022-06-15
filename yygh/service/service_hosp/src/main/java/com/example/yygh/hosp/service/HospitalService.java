package com.example.yygh.hosp.service;

import com.example.yygh.model.hosp.Hospital;
import com.example.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface HospitalService {
    //上传医院接口
    void save(Map<String, Object> paramMap);

    Hospital getByHoscode(String hoscode);


    //更新医院上线状态
    void updateStatus(String id, Integer status);


    //获取医院名称
    String getHospName(String hoscode);

    //根据医院名称查询
    List<Hospital> findByHosname(String hosname);

    //根据医院编号获取医院预约挂号详情
    Map<String, Object> getHospById(String id);

    Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);

    //根据医院编号获取医院预约挂号详情
    Map<String, Object> item(String hoscode);
}
