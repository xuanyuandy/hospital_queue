package com.example.yygh.order.api;


import com.example.yygh.common.result.Result;
import com.example.yygh.order.service.AlipayService;
import com.example.yygh.order.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order/alipay")
public class AlipayController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayService alipayService;

    //生成支付宝支付二维码
    @GetMapping("createNative/{orderId}")
    public Result createNative(@PathVariable Long orderId) {
        Map map = alipayService.createNative(orderId);
        return Result.ok(map);
    }

    //查询支付状态
    @GetMapping("queryPayStatus/{orderId}")
    public Result queryPayStatus(@PathVariable Long orderId) {
        //调用微信接口实现支付状态查询
        Map<String,String> resultMap = alipayService.queryPayStatus(orderId);
        //判断
        if(resultMap == null) {
            return Result.fail().message("支付出错");
        }
        // 支付成功标志
        if("TRADE_SUCCESS".equals(resultMap.get("trade_state"))) { //支付成功
            //更新订单状态
            String out_trade_no = resultMap.get("out_trade_no");//订单编码
            paymentService.paySuccess(out_trade_no,resultMap,1);
            return Result.ok().message("支付成功");
        }
        return Result.ok().message("支付中");
    }
}
