package com.wms.applicationInfra.config;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionTimeLogger {

//	@Around("execution(* com.nbs.nbs.services.alarm.NBSVB010_AlarmView.AlarmViewController.findScopeWithPagination(..))")
//	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
//		long start = System.currentTimeMillis();
//
//		Object proceed = joinPoint.proceed(); // 실제 메소드 실행
//
//		long executionTime = System.currentTimeMillis() - start;
//
//		System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
//
//		return proceed;
//	}
}
