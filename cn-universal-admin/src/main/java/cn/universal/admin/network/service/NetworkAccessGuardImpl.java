package cn.universal.admin.network.service;

import cn.universal.core.network.NetworkAccessGuard;
import org.springframework.stereotype.Component;

@Component
public class NetworkAccessGuardImpl implements NetworkAccessGuard {

  private final INetworkService networkService;

  public NetworkAccessGuardImpl(INetworkService networkService) {
    this.networkService = networkService;
  }

  @Override
  public void assertAccess(Long networkId) {
    if (networkService.selectNetworkById(networkId) == null) {
      throw new IllegalArgumentException("Network component does not exist");
    }
  }
}
