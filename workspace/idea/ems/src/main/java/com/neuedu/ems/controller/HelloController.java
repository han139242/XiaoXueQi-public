package com.neuedu.ems.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "欢迎使用 EMS 系统！";
    }

    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello, World!";
    }
}
