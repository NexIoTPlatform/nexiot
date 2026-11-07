package cn.universal.http.access.client;

import cn.universal.http.access.model.HttpClientResponse;

/** Response returned by a product-driver-directed third-party interaction. */
public record HttpInteractionResult(HttpClientResponse response, String normalizedPayload) {}
