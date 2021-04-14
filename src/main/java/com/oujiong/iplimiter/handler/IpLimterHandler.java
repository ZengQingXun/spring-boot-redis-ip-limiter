package com.oujiong.iplimiter.handler;

import cn.edu.idea.util.HttpUtils;
import com.google.common.base.Preconditions;
import com.oujiong.iplimiter.annotation.IpLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 限流处理器
 *
 * @author xub
 * @date 2019/10/27 1:17
 */
@Aspect
@Component
public class IpLimterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpLimterHandler.class);

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * getRedisScript 读取脚本工具类
     * 这里设置为Long,是因为ipLimiter.lua 脚本返回的是数字类型
     */
    private DefaultRedisScript<Long> getRedisScript;

    @PostConstruct
    public void init() {
        getRedisScript = new DefaultRedisScript<>();
        getRedisScript.setResultType(Long.class);
        getRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("ipLimiter.lua")));
        LOGGER.info("IpLimterHandler[分布式限流处理器]脚本加载完成");
    }

    @Around("@annotation(ipLimiter)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, IpLimiter ipLimiter) throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("IpLimterHandler[分布式限流处理器]开始执行限流操作");
        }
        Signature signature = proceedingJoinPoint.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("the Annotation @IpLimter must used on method!");
        }
        /**
         * 获取注解参数
         */
        // 限流模块IP
        Object[] args = proceedingJoinPoint.getArgs();
        String limitIp = null;
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                limitIp = HttpUtils.getIPAddress((HttpServletRequest) arg);
                break;
            }
        }
        Preconditions.checkNotNull(limitIp);
        // 限流阈值
        long limitTimes = ipLimiter.limit();
        // 限流超时时间
        long expireTime = ipLimiter.time();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("IpLimterHandler[分布式限流处理器]参数值为-limitTimes={},limitTimeout={}", limitTimes, expireTime);
        }
        // 限流提示语
        String message = ipLimiter.message();
        /**
         * 执行Lua脚本
         */
        List<String> ipList = new ArrayList();
        // 设置key值为注解中的值
        ipList.add(limitIp);
        /**
         * 调用脚本并执行
         */
        Long result = (Long) redisTemplate.execute(getRedisScript, ipList, expireTime, limitTimes);
        if (result==null || result == 0) {
            String msg = "由于超过单位时间=" + expireTime + "-允许的请求次数=" + limitTimes + "[触发限流]";
            LOGGER.debug(msg);
            // 达到限流返回给前端信息
            return message;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("IpLimterHandler[分布式限流处理器]限流执行结果-result={},请求[正常]响应", result);
        }
        return proceedingJoinPoint.proceed();
    }
}
