package com.global.order_api.core.aspect;

import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Log4j2
public class PerformanceAspect {
		// fully monitor the function
		// before and in and after
		// execution => means is second of execution of function
		// and give him filter expression (point cut) to determine this advice scope will be
		// return type + path + name of class +name of function + parameters of function
	@Around("@annotation(com.global.order_api.core.annotation.TrackExecutionTime)")
	// throwable => because any method can throw exception
	// so spring tell us that if the original method throw exception
	// .proceed => will rethrows the exception again
	public Object logExecutionTime(ProceedingJoinPoint jointPoint) throws Throwable{
		MethodSignature	methodSignature=(MethodSignature) jointPoint.getSignature();
		String className=methodSignature.getDeclaringType().getSimpleName();
		String methodName = methodSignature.getName();
		
		StopWatch stopWatch=new StopWatch();
		stopWatch.start();
		// object => because we don't know which type method will return
		// .proceed()=> tell spring start to execute the function 
		Object result=jointPoint.proceed();
		stopWatch.stop();
		log.info("⏱️ Execution Time | Class:"
				+ " [{}] | Method: [{}] | Took: {} ms", 
                className, methodName, stopWatch.getTime());

       return result;
	}
}
