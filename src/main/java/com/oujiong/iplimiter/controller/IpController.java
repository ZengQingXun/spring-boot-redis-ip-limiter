package com.oujiong.iplimiter.controller;

import com.oujiong.iplimiter.annotation.IpLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description: 接口测试
 *
 * @author xub
 * @date 2019/6/4 上午9:10
 */
@RequestMapping("/ip")
@RestController
public class IpController {

    private static final String MESSAGE = "请求失败,你的IP访问太频繁";

    @PostMapping("/iplimiter")
    @IpLimiter(limit = 5, time = 10, message = MESSAGE)
    public String sendPayment(HttpServletRequest request) throws Exception {
        return "请求成功";
    }

}
