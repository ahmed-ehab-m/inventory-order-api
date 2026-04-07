package com.global.order_api.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // this annotation will be put on methods only
@Retention(RetentionPolicy.RUNTIME) // SB will execute it in run time
public @interface TrackExecutionTime {

}
