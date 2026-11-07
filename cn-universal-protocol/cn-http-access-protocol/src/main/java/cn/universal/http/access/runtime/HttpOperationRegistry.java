package cn.universal.http.access.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Lock-free read registry for active endpoint access codes. */
@Component
public class HttpOperationRegistry {

  private final AtomicReference<Map<String, HttpRouteSnapshot>> snapshots =
      new AtomicReference<>(Map.of());

  public synchronized void replace(HttpRouteSnapshot snapshot) {
    Map<String, HttpRouteSnapshot> next = new HashMap<>(snapshots.get());
    String accessCode = snapshot.config().getAccessCode();
    HttpRouteSnapshot existing = next.get(accessCode);
    if (existing != null && !existing.definition().id().equals(snapshot.definition().id())) {
      throw new IllegalArgumentException("accessCode is already used by another HTTP network");
    }
    next.entrySet().removeIf(entry -> entry.getValue().definition().id().equals(snapshot.definition().id()));
    next.put(accessCode, snapshot);
    snapshots.set(Collections.unmodifiableMap(next));
  }

  public synchronized void remove(Long networkId) {
    Map<String, HttpRouteSnapshot> next = new HashMap<>(snapshots.get());
    next.entrySet().removeIf(entry -> entry.getValue().definition().id().equals(networkId));
    snapshots.set(Collections.unmodifiableMap(next));
  }

  public Optional<HttpRouteSnapshot> getByAccessCode(String accessCode) {
    return Optional.ofNullable(snapshots.get().get(accessCode));
  }

  public Optional<HttpRouteSnapshot> getByNetworkId(Long networkId) {
    return snapshots.get().values().stream()
        .filter(snapshot -> snapshot.definition().id().equals(networkId)).findFirst();
  }

  public Optional<HttpRouteSnapshot> getByUnionId(String unionId) {
    return snapshots.get().values().stream()
        .filter(snapshot -> snapshot.definition().unionId().equals(unionId)).findFirst();
  }

  public java.util.List<HttpRouteSnapshot> all() {
    return new ArrayList<>(snapshots.get().values());
  }
}
