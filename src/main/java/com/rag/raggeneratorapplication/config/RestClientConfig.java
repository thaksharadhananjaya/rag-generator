package com.rag.raggeneratorapplication.config;

import com.rag.raggeneratorapplication.config.props.OllamaProperties;
import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP clients for the AI providers. One is selected at runtime via
 * {@code app.ai.provider}; adapters inject the client by bean name
 * ({@code openAiRestClient} / {@code ollamaRestClient}). Both beans are always
 * created (they only build configuration, no connection), so this class carries
 * no provider condition.
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient openAiRestClient(OpenAiProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory(props.timeoutSeconds()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + nullToEmpty(props.apiKey()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestClient ollamaRestClient(OllamaProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory(props.timeoutSeconds()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return factory;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
