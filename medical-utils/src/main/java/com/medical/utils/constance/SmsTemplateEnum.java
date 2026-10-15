package com.medical.utils.constance;

//短信模版的枚举类
public enum SmsTemplateEnum {

//    本质上调用枚举类的构造方法
    QF_TEMPLATE("SMS_4673928","千峰互联");

    private String code;
    private String name;

    private SmsTemplateEnum(String code,String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
}
