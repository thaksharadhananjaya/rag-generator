package com.rag.raggeneratorapplication.config;

import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the OpenAI-compatible API. Isolated here so the LLM and
 * embedding adapters share one configured client, and so pointing at a different
 * provider (Azure OpenAI, a local gateway) is a single base-URL change.
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient openAiRestClient(OpenAiProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + nullToEmpty(props.apiKey()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
