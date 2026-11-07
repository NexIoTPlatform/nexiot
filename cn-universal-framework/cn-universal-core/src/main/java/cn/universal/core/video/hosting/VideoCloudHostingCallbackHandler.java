package cn.universal.core.video.hosting;

/** Bridge for vendor callback controllers to hand hosting events to the admin service. */
public interface VideoCloudHostingCallbackHandler {
  /**
   * Handles a raw vendor callback when it belongs to a hosting session.
   *
   * @return true when the callback was a hosting event and was processed
   */
  boolean handle(String platform, String body);

  /** Query callbacks also include console probes without a hosting session. */
  default void handleQuery(String platform, java.util.Map<String, String> query) {
    handle(platform, cn.hutool.json.JSONUtil.toJsonStr(query));
  }

  /**
   * Handles vendor revocation callbacks that do not carry a hosting-session state.
   * Implementations that do not need out-of-band revocation can keep the default.
   */
  default boolean handleRevocation(String platform, String body) {
    return false;
  }
}
