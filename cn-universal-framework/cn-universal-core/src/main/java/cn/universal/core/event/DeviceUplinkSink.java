package cn.universal.core.event;

import java.util.function.Supplier;

/** Snapshot suppliers are call-scoped only. Ordinary observers must not wait for I/O.
 * A durable sink explicitly opts into synchronous acceptance so storage failure is observable.
 */
public interface DeviceUplinkSink {
  default boolean durableAcceptance() { return false; }
  void accept(DeviceUplinkEvent.Identity identity, Supplier<DeviceUplinkEvent> snapshot);
}
