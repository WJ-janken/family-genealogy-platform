package com.family.genealogy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web配置类，用于设置字符编码
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加字符串消息转换器并设置字符编码
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
        
        // 如果没有找到字符串转换器，添加一个
        if (converters.stream().noneMatch(c -> c instanceof StringHttpMessageConverter)) {
            converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        }
    }
}