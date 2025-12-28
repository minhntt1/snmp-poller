package com.home.network.statistic.poller.igate.gw240.in;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class WebRequestInfo {
    private static final String URL_STATUS_WIFI_STATION = "/cgi-bin/status_wifi_station.asp";
    private static final String URL_INDEX = "/cgi-bin/index.asp";
    private static final String URL_HOST_PATTERN = "https://%s";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String SET_COOKIE_HEADER = "set-cookie";

    private final String host;
    private final Map<String,String> headers = new HashMap<>();

    public static String extractSessionFromCookie(String cookie) {
        var cookieSplit = cookie.split(";");
        for (var c : cookieSplit) if (c.contains("SESSIONID")) return c.strip();
        return "SESSIONID=";
    }

    private String createValueHeaderCookie(String cookieBase64Header, String cookieSessionHeader) {
        return "base64=" + cookieBase64Header + "; SESSIONID=" + cookieSessionHeader;
    }

    public WebRequestInfo(String host, String cookieBase64Header) {
        this.host = host;
        this.headers.put(COOKIE_HEADER, createValueHeaderCookie(cookieBase64Header, ""));
    }

    public WebRequestInfo(String host, String cookieBase64Header, String cookieSessionHeader) {
        this.host = host;
        this.headers.put(COOKIE_HEADER, createValueHeaderCookie(cookieBase64Header, cookieSessionHeader));
    }

    public void updateCookieSessionId(String responseCookie) {
        var newSessIdString = extractSessionFromCookie(responseCookie);
        var newCookie = headers.get(COOKIE_HEADER).replaceFirst("SESSIONID=.*", newSessIdString);
        headers.put(COOKIE_HEADER, newCookie);
    }

    public String obtainHostUrlIndex() {
        return URL_HOST_PATTERN.concat(URL_INDEX).formatted(host);
    }

    public String obtainHostUrlWifiStation() {
        return URL_HOST_PATTERN.concat(URL_STATUS_WIFI_STATION).formatted(host);
    }

    public void addHeader(HttpHeaders httpHeaders) {
        for (var header : headers.entrySet()) {
            httpHeaders.add(header.getKey(), header.getValue());
        }
    }
}
