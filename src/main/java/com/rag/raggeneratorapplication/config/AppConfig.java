package com.rag.raggeneratorapplication.config;

import com.rag.raggeneratorapplication.config.props.DocumentProperties;
import com.rag.raggeneratorapplication.config.props.MinioProperties;
import com.rag.raggeneratorapplication.config.props.OllamaProperties;
import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import com.rag.raggeneratorapplication.config.props.ProcessingProperties;
import com.rag.raggeneratorapplication.config.props.RetrievalProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the application's typed configuration properties. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        OpenAiProperties.class,
        OllamaProperties.class,
        MinioProperties.class,
        RetrievalProperties.class,
        ProcessingProperties.class,
        DocumentProperties.class
})
public class AppConfig {
}
