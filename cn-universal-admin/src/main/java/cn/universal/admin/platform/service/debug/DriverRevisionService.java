package cn.universal.admin.platform.service.debug;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProtocolRevision;
import cn.universal.persistence.mapper.IoTProtocolRevisionMapper;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DriverRevisionService {
  private final IoTProtocolRevisionMapper mapper;

  public DriverRevisionService(IoTProtocolRevisionMapper mapper) {
    this.mapper = mapper;
  }

  public static String source(IoTDeviceProtocol protocol) {
    return JSONUtil.parseObj(protocol.getConfiguration()).getStr("location", "");
  }

  public static String hash(String source) {
    return DigestUtil.sha256Hex(source);
  }

  public void checkExpected(IoTDeviceProtocol protocol, String expected) {
    if (expected != null && !hash(source(protocol)).equals(expected)) {
      throw new IoTException("驱动已被其他窗口更新，请复制当前草稿后重新打开，避免覆盖他人的修改");
    }
  }

  /** 在驱动保存事务中调用；首次更新保留原版本，并最多保留最近 30 份源码。 */
  public void recordChange(IoTDeviceProtocol before, String source, String version, String actor) {
    List<IoTProtocolRevision> revisions = mapper.list(before.getId());
    String previous = source(before);
    long time =
        Math.max(
            System.currentTimeMillis(),
            revisions.stream()
                    .map(IoTProtocolRevision::getCreatedAt)
                    .filter(Objects::nonNull)
                    .mapToLong(Date::getTime)
                    .max()
                    .orElse(0)
                + 1);
    if (revisions.isEmpty()) insert(before.getId(), previous, before.getVersion(), actor, time++);
    if (!previous.equals(source)) insert(before.getId(), source, version, actor, time);
    List<IoTProtocolRevision> all = mapper.list(before.getId());
    for (int i = 30; i < all.size(); i++) mapper.delete(all.get(i).getId(), before.getId());
  }

  private void insert(String protocolId, String source, String version, String actor, long time) {
    IoTProtocolRevision revision = new IoTProtocolRevision();
    revision.setId(UUID.randomUUID().toString());
    revision.setProtocolId(protocolId);
    revision.setSource(source);
    revision.setSourceHash(hash(source));
    revision.setVersion(version);
    revision.setSavedBy(actor);
    revision.setCreatedAt(new Date(time));
    mapper.insert(revision);
  }

  public List<IoTProtocolRevision> list(String protocolId) {
    return mapper.list(protocolId);
  }

  public void delete(String protocolId) {
    mapper.deleteAll(protocolId);
  }
}
