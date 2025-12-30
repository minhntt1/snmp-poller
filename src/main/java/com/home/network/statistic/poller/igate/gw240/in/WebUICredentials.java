package com.home.network.statistic.poller.igate.gw240.in;

import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WebUICredentials {
    private static final String AUTH_PATTERN = "uid =%s; psw=%s";
    private String user;
    private String pass;
    private String host;

    public String encodeBase64() {
        return Base64.getEncoder().encodeToString(AUTH_PATTERN.formatted(this.user, this.pass).getBytes(StandardCharsets.UTF_8));
    }

    public WebRequestInfo obtainAuthInfo() {
        return new WebRequestInfo(host, encodeBase64());
    }

    public WebRequestInfo obtainAuthInfo(String sessionHeader) {
        return new WebRequestInfo(host, encodeBase64(), sessionHeader);
    }
}
