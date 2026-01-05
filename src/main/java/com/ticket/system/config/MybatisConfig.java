package com.ticket.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ticket.system.mapper")
public class MybatisConfig {
}