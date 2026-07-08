package com.neuedu.ems.common;

import java.util.HashMap;

public class ResultData extends HashMap<String, Object> {
    private int code;

    private String msg;

    private Object data;

    public ResultData(ResultCode resultCode, Object data) {


        this.code = resultCode.getCode();
        this.msg = resultCode.getMessage();
        this.data = data;

        this.put("code", code);
        this.put("msg", msg);
        this.put("data", data);
    }

    public ResultData(ResultCode resultCode) {
        this(resultCode, null);
    }

    public static ResultData success(ResultCode resultCode, Object data) {
        return new ResultData(resultCode, data);
    }

    public static ResultData success(ResultCode resultCode) {
        return success(resultCode, null);
    }

    public static ResultData fail(ResultCode resultCode, Object data) {
        return new ResultData(resultCode, data);
    }

    public static ResultData fail(ResultCode resultCode) {
        return new ResultData(resultCode);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
