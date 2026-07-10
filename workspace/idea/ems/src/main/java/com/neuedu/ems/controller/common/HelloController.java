package com.neuedu.ems.controller.common;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping
    public ResultData hello() {
        return ResultData.success(ResultCode.SUCCESS, "欢迎使用 EMS 系统！");
    }
}
