package com.example.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.example.yygh.enums.PaymentTypeEnum;
import com.example.yygh.enums.RefundStatusEnum;
import com.example.yygh.model.order.OrderInfo;
import com.example.yygh.model.order.PaymentInfo;
import com.example.yygh.model.order.RefundInfo;
import com.example.yygh.order.service.AlipayService;
import com.example.yygh.order.service.OrderService;
import com.example.yygh.order.service.PaymentService;
import com.example.yygh.order.service.RefundInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AlipayServiceImpl implements AlipayService {
    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RefundInfoService refundInfoService;

    @Override
    public Map createNative(Long orderId) {
        try{
            //从redis获取数据
//            Map payMap = (Map)redisTemplate.opsForValue().get(orderId.toString());
//            if(payMap != null) {
//                return payMap;
//            }
            //1 根据orderId获取订单信息
            OrderInfo order = orderService.getById(orderId);
            //2 向支付记录表添加信息
            paymentService.savePaymentInfo(order, PaymentTypeEnum.ALIPAY.getStatus());

            // 支付宝请求
            String app_id = "??";
            String private_key = "???";
            String alipayPublicKey = "??";
            AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipaydev.com/gateway.do",app_id,private_key,"json","utf-8",alipayPublicKey,"RSA2");
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();

            request.setNotifyUrl("");
            JSONObject bizContent = new JSONObject();

            System.out.println("order  " + order.getOutTradeNo());
            bizContent.put("out_trade_no", order.getOutTradeNo());
            bizContent.put("total_amount", 1);
            bizContent.put("subject", "test");

            request.setBizContent(bizContent.toString());
            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            // 通过response返回参数判断订单是否已经生成
            // 封装返回结果集
            Map map = new HashMap<>();
            map.put("orderId", orderId);
            map.put("totalFee", order.getAmount());
            map.put("resultCode", response.getCode());
            map.put("codeUrl", response.getQrCode()); //二维码地址

            if(response.isSuccess()){
                redisTemplate.opsForValue().set(orderId.toString(),map,120, TimeUnit.MINUTES);
            }
            return map;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, String> queryPayStatus(Long orderId) {
        try {
            //1 根据orderId获取订单信息
            OrderInfo orderInfo = orderService.getById(orderId);

            String OutTradeNo = orderInfo.getOutTradeNo();
            String app_id = "??";
            String private_key = "???";
            String alipayPublicKey = "??";
            AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipaydev.com/gateway.do",app_id,private_key,"json","utf-8",alipayPublicKey,"RSA2");
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{" +
                    "  \"out_trade_no\":\"" + OutTradeNo + "\"" +
                    "}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            Map<String,String> map = new HashMap<>();
            map.put("code",response.getCode());
            map.put("trade_state",response.getTradeStatus());
            map.put("out_trade_no",OutTradeNo);
            // code = 10000 success
            return map;
        }catch(Exception e) {
            return null;
        }
    }

    @Override
    public Boolean refund(Long orderId) {
        try {

            //获取支付记录信息
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderId, PaymentTypeEnum.ALIPAY.getStatus());
            //添加信息到退款记录表
            RefundInfo refundInfo = refundInfoService.saveRefundInfo(paymentInfo);
            //判断当前订单数据是否已经退款
            if(refundInfo.getRefundStatus().intValue() == RefundStatusEnum.REFUND.getStatus().intValue()) {
                return true;
            }

            // 先查询一次获取trade_no total_amount
            OrderInfo orderInfo = orderService.getById(orderId);

            String OutTradeNo = orderInfo.getOutTradeNo();
            String app_id = "2021000118637747";
            String private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCa1v56/+FMKTCD+oO++kLfeZYq4CfCqSewU9LOJbn99x4eCiJEdZ0fctW7441VScghVPN2Otg8A/BKyxfVQJfYX5eMat9EKo1BE50hnoHuLKiGwrZyAhJs3iaUswDLAVju/Fcx7RQ8dD4naLdq9k87kGRDlZ9J+OCOXiubyA5KWy0Tvhu8ejuwGvIEGoPyS/JFyHF1XiAsNjtmGcA95b/9exAe0R+Mn7tRMxUe7dIC7ciTnErWorli33p2JSzLlQAdoHiOiq3mKi7xsJsAaUViJDpGo4ysLEkRhYyv9nTX8vn9tnIVMkY+DabVCXl5oiY8yxKzSVu+TX3E9pcrynDDAgMBAAECggEAAYuPso6HIwluMIL5eZhpvm0qMHdYLjsd6EaN3fzGZGBo7ofPW7uIu92bgGp+6JD57Es6ur1Plxm69iQcssYYPdKGYqJWZAnAqxuU/9bjGQtdCys6Qtz+bhOxct5ubZgv2QKvpBHnl8rZK++BlRGsLL3+IiaGFmjjZMhPyxgt/FP0nF7C7eX6NJLpkk10VQH5n+KO8v4kMMMaZY+yxUAEC21/BY/MpZ9ULF59/9EYpXTrfagIim9zxrTV/QBtftEpmFOe1LUULBgq0mfmn37gnHaunffNVDfDn0ZPYxym+u6qxjdsdGC0752cuUbebwuOw6DsP15l5OADG2V1AH/8MQKBgQDYgvvH2LBjGmH/1AUuCy3kkv1uKkWj3SVuRY+kHZTV522rwU7i0A6hrie402H7TS39yl/c/axq4rowStlO6G291yyTJEfPv5eNq+INrlxne/NvDP+PUkeEHy2FyvGiloZTLoDsS6g+cg2SXcFHyDxiBR2qlqBc8VSf5XFrdt8CTwKBgQC3FIcae0+tT5vsO2zE492cPIHhBcaafL+2KeWTiOnZ3RULfICW/lZ26s7vWsZffx4mKCZucmiGBccSpgt5+ZlltFy9XDRuhfNZBhxvzrkOsgO/OkJn0HZyVpQMmdjxu7DgZXGKj+9KNwof3G6rEnoyI3H06k2wA89hoKtgLRiRTQKBgQCmPJOzEtYDkYszEQhVHPJEsPNy9XP45+BIdZJfUPMo9YNIP6khDtxdGUmYaHyvpcetOAg4tnD+tEx+rcmCA4XXH/Iym8WjZhioBxQOqxR5xDrZxiImWeddM08RjgvPaUGDdo44X7KzF4ECI4g2ZDCwlfr8MqC4G/d8uX9HIsMBkwKBgB/nrue5h5gfYH/Zl0n0n+EjVkr9s6+mf85iHMUKPh6efZVj3BD0P+M3ZlqmCh4ITBvAMXpb68U3aorgbu2Rxt7HgMXrNKPyKgdgnkIJ14wtD6QapgnrdUjVt7U+dmUiHV/trczE6qGXV+dXhd3AOZVVQf25e1JqR5Cu7E61bSudAoGBAJr2klgAHUhKCkAFtZbqyWFYQE00OAF8OxY4hC8RBFo300YDpoFk1RN3YX/zNH1O+hzNz4tMoB79C0zqNoY8WHk/7RmIovQKFRfQ4WaR+DR62VtTFcZ3gPFrXfE3WOu5E+SE2YCLdQB9qke83FZYfbnkw4K1eHsmeJW2idc/ZqH0";
            String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp07scwmhYH70V0y9jz+tqfHVGjrLrvfiBW1VOXLAbbaHUugl6KzSJYwvNR03zVi7JUClC6CwOoyw6/+eQxtAL2TD5PLjEujkFbQ5GhekbBd7pG+9yfojh2H/kqkb0NUQxWRKDRxVZx0VjSLk9gciIIVsxX6g/TXsAvFW8atOow2nf+ANDl1X+lUf4UapSHZQjAXJzgKbEYexGevKIVnnyOfIy0h7djRd3wv0fHiHZ+FXocHFtEGkNlmcd8kr+YG/1mbM1lCTy9Q4ycDw9QbyBMFtWiccDKIzDmxAov/zUgexf0KuOiemCq4s4ZTt5KsmdNA1MlRc5sSUQ4ZvrokvxQIDAQAB";
            AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipaydev.com/gateway.do",app_id,private_key,"json","utf-8",alipayPublicKey,"RSA2");
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{" +
                    "  \"out_trade_no\":\"" + OutTradeNo + "\"" +
                    "}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            // 再次请求获取参数
            String trade_no = response.getTradeNo();
            String refund_amount = response.getTotalAmount();
            AlipayTradeRefundRequest request2 = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("trade_no", trade_no);
            bizContent.put("refund_amount", refund_amount);
            bizContent.put("out_request_no", trade_no);


            request2.setBizContent(bizContent.toString());
            AlipayTradeRefundResponse response2 = alipayClient.execute(request2);

            if(response2.isSuccess()){
                refundInfo.setCallbackTime(new Date());
                refundInfo.setTradeNo(response2.getTradeNo());
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());
                refundInfo.setCallbackContent("ok");
                refundInfoService.updateById(refundInfo);
                return true;
            }
            return false;

        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
