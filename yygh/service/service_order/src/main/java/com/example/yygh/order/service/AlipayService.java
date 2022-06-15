package com.example.yygh.order.service;

import java.util.Map;

public interface AlipayService {
    // 生成支付二维码
    Map createNative(Long orderId);

    // 调用微信接口实现支付状态查询
    Map<String, String> queryPayStatus(Long orderId);

    // 退款处理
    Boolean refund(Long orderId);

}
