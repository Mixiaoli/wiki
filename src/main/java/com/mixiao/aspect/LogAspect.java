package com.mixiao.aspect;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.mixiao.util.RequestContext;
import com.mixiao.util.SnowFlake;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component//交给spring管理
public class LogAspect {

    private final static Logger LOG = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 定义一个切点
     */
    @Pointcut("execution(public * com.mixiao.controller..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Resource
    private SnowFlake snowFlake;

    @Before("controllerPointcut()") //前置通知
    public void doBefore(JoinPoint joinPoint) throws Throwable {

        // 增加日志流水号
        MDC.put("LOG_ID", String.valueOf(snowFlake.nextId()));

        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Signature signature = joinPoint.getSignature();
        String name = signature.getName();

        // 打印请求信息
        LOG.info("------------- 开始 -------------");
        LOG.info("请求地址: {} {}", request.getRequestURL().toString(), request.getMethod());
        LOG.info("类名方法: {}.{}", signature.getDeclaringTypeName(), name);
        LOG.info("远程地址: {}", request.getRemoteAddr());
        //获取远程IP
        RequestContext.setRemoteAddr(getRemoteIp(request));

        // 打印请求参数
        Object[] args = joinPoint.getArgs();//join-连接点 拿到使用的参数
        // LOG.info("请求参数: {}", JSONObject.toJSONString(args));

        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (isMultipartType(arg)
                    || arg instanceof ServletRequest
                    || arg instanceof ServletResponse) {
                continue;
            }
            arguments[i] = arg;
        }
        // 排除字段，敏感字段或太长的字段不显示
        String[] excludeProperties = {"password", "file"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        LOG.info("请求参数: {}", JSONObject.toJSONString(arguments, excludefilter));
    }
    private boolean isMultipartType(Object arg) {
        if (arg == null) {
            return false;
        }
        if (arg instanceof MultipartFile) {
            return true;
        }
        if (arg instanceof MultipartFile[]) {
            return true;
        }
        if (arg.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(arg);
            for (int i = 0; i < length; i++) {
                Object element = java.lang.reflect.Array.get(arg, i);
                if (isMultipartType(element)) {
                    return true;
                }
            }
        }
        if (arg instanceof Iterable) {
            for (Object element : (Iterable<?>) arg) {
                if (isMultipartType(element)) {
                    return true;
                }
            }
        }
        if (arg instanceof java.util.Map) {
            for (Object value : ((java.util.Map<?, ?>) arg).values()) {
                if (isMultipartType(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        // 排除字段，敏感字段或太长的字段不显示
        String[] excludeProperties = {"password", "file"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        LOG.info("返回结果: {}", JSONObject.toJSONString(result, excludefilter));
        LOG.info("------------- 结束 耗时：{} ms -------------", System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 使用nginx做反向代理，需要用该方法才能取到真实的远程IP
     *
     * @param request
     * @return
     */
    public String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
