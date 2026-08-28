package com.example.bookmark.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ZhipuProperties.class, KimiProperties.class})
public class AiConfig {
}
