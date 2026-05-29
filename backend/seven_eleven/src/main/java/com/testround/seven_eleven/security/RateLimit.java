package com.testround.seven_eleven.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    long requests();
    long perSeconds() default 60; // Chu kỳ giới hạn tính theo giây, mặc định là 60 giây (1 phút)
}
