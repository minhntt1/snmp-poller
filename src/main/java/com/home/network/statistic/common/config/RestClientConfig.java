package com.home.network.statistic.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient insecureRestClient() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager trustAll = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{ trustAll }, new SecureRandom());

        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        HttpClient client = HttpClient.newBuilder()
                .sslContext(ctx)
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(client))
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> {})
                .build();
    }
}
