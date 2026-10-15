package com.medical.utils.common;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import com.medical.utils.config.AliyunProperties;
import com.medical.utils.core.SpringBeanUtils;

/**
 * 发送短信工具类（阿里云短信）。
 * AccessKey 等敏感配置从 aliyun.sms.* 读取，不再硬编码在代码中。
 */
public class AliSmsUtils {

    private static volatile IAcsClient client;

    /**
     * 懒加载获取短信客户端
     */
    private static IAcsClient getClient() {
        if (client == null) {
            synchronized (AliSmsUtils.class) {
                if (client == null) {
                    AliyunProperties.Sms sms = SpringBeanUtils.getBean(AliyunProperties.class).getSms();
                    DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou",
                            sms.getAccessKeyId(), sms.getAccessKeySecret());
                    client = new DefaultAcsClient(profile);
                }
            }
        }
        return client;
    }

    /**
     * 发送短信
     *
     * @param signName     短信签名
     * @param templateCode 短信模板 Code
     * @param phone        接收手机号
     * @param code         验证码内容
     */
    public static void sendSms(String signName, String templateCode, String phone, String code) {
        SendSmsRequest request = new SendSmsRequest();
        request.setSignName(signName);
        request.setTemplateCode(templateCode);
        request.setPhoneNumbers(phone);
        request.setTemplateParam("{\"code\":\"" + code + "\"}");

        try {
            SendSmsResponse response = getClient().getAcsResponse(request);
            System.out.println(new Gson().toJson(response));
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            // 记录错误日志
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());
        }
    }
}