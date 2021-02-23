package com.fenglinga.tinyspring.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.mina.http.api.HttpMethod;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {
	String name() default "";

	@AliasFor("path")
	String[] value() default {};
	
	HttpMethod[] method() default {};
}
