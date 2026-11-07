package cn.universal.http.access.pipeline;

public record HttpUpstreamResult(int acceptedRecords, Object replyPayload) {}
