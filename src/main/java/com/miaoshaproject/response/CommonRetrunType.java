package com.miaoshaproject.response;

public class CommonRetrunType {

    //表明对应请求的返回处理结果是success或fail
    private String status;

    //若status=success，data内返回前端需要的json数据
    //若status=fail，data内返回通用的错误码格式
    private Object data;

    public static CommonRetrunType create(Object data){
        return CommonRetrunType.create(data,"success");
    }

    public static CommonRetrunType create(Object data,String status){
        CommonRetrunType type = new CommonRetrunType();
        type.setData(data);
        type.setStatus(status);
        return type;
    }




    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }


}
