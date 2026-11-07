package cn.universal.core.network;

/** Immutable network definition passed from the management layer to protocol runtimes. */
public record NetworkRuntimeDefinition(
    Long id,
    String type,
    String productKey,
    String unionId,
    String name,
    String configuration,
    Integer state) {}
