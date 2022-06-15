package com.example.yygh.msm.service.impl;

import com.example.yygh.common.helper.HttpUtils;
import com.example.yygh.msm.service.MsmService;
import com.example.yygh.vo.msm.MsmVo;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {
    @Override
    public boolean send(String phone, String code) {
        System.out.println(phone);
        System.out.println(code);
        //判断手机号是否为空
        if (StringUtils.isEmpty(phone)) {
            return false;
        }
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "???";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);
        querys.put("param", "dydydy**code**:" + code+ ",**minute**:5");
        querys.put("smsSignId", "???");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            return true;
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean send(MsmVo msmVo) {
        if(!StringUtils.isEmpty(msmVo.getPhone())) {
            String phone = msmVo.getPhone();
            //判断手机号是否为空
            if(StringUtils.isEmpty(phone)) {
                return false;
            }
            System.out.println(phone);
            String host = "https://gyytz.market.alicloudapi.com";
            String path = "/sms/smsSend";
            String method = "POST";
            String appcode = "???";
            Map<String, String> headers = new HashMap<String, String>();
            //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
            headers.put("Authorization", "APPCODE " + appcode);
            Map<String, String> querys = new HashMap<String, String>();
            querys.put("mobile", phone);
            querys.put("param", "sbsbsbsuccess???");
            querys.put("smsSignId", "???");
            querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
            Map<String, String> bodys = new HashMap<String, String>();

            try {
                HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
                System.out.println(response.toString());
                return true;
                //获取response的body
                //System.out.println(EntityUtils.toString(response.getEntity()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

}
