package cn.universal.core.network;

/** Ensures the current management principal can access a network component. */
public interface NetworkAccessGuard {
  void assertAccess(Long networkId);
}
