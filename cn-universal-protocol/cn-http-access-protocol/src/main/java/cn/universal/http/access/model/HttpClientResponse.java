package cn.universal.http.access.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record HttpClientResponse(
    int status, Map<String, List<String>> headers, String body, URI uri, long durationMs) {}
