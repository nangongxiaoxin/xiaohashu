package com.slilio.framework.biz.operationlog.aspect;

import com.slilio.framework.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Slf4j
public class ApiOperationLogAspect {
    //以自定义注解@ApiOperationLog注解为切点，凡事添加ApiOperationLog的方法，都会执行环绕的代码
    @Pointcut("@annotation(com.slilio.framework.biz.operationlog.aspect.ApiOperationLog)")
    public void apiOperationLog() {}

    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        //请求开始时间
        long startTime = System.currentTimeMillis();

        //获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        //请求入参
        Object[] args = joinPoint.getArgs();
        //入参转JSON字符串
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(", "));

        //功能描述信息
        String description = getApiOperationLogDescription(joinPoint);

        // 打印请求相关参数
        log.info("====== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} =================================== ",description, argsJsonStr, className, methodName);

        //执行切点方法
        Object result = joinPoint.proceed();

        //执行耗时
        long executionTime = System.currentTimeMillis() - startTime;

        // 打印出参等相关信息
        log.info("====== 请求结束: [{}], 耗时: {}ms, 出参: {} =================================== ",description, executionTime, JsonUtils.toJsonString(result));

        return result;
    }

    /**
     * 获取注解的描述信息
     * @param joinPoint
     * @return
     */
    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        //从ProceedingJoinPoint获取MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //使用MethodSignature获取当前被注解的Method
        Method method = signature.getMethod();
        //从Method中提取LogExecution注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        //从LogExecution注解中获取description信息
        return apiOperationLog.description();
    }

    /**
     * 转JSON字符串
     */
    private Function<Object, String> toJsonStr()  {
        return JsonUtils::toJsonString;
    }
}
