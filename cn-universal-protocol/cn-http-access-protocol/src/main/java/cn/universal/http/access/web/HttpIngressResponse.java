package cn.universal.http.access.web;

import java.util.Map;

public record HttpIngressResponse(int status, Map<String, String> headers, Object body) {}
