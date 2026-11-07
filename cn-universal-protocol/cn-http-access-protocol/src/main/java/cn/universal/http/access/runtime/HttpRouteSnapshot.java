package cn.universal.http.access.runtime;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.http.access.config.HttpIntegrationConfig;
import java.net.http.HttpClient;

/** Immutable active HTTP network, one endpoint and one bound product. */
public record HttpRouteSnapshot(
    NetworkRuntimeDefinition definition, HttpIntegrationConfig config, HttpClient httpClient) {}
