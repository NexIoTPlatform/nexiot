package cn.universal.persistence.mapper;

import cn.universal.persistence.entity.IoTProtocolRevision;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface IoTProtocolRevisionMapper {
  @Insert(
      "INSERT INTO iot_protocol_revision (id, protocol_id, source, source_hash, version, saved_by,"
          + " created_at) VALUES (#{id}, #{protocolId}, #{source}, #{sourceHash}, #{version},"
          + " #{savedBy}, #{createdAt})")
  int insert(IoTProtocolRevision revision);

  @Select(
      "SELECT id, protocol_id AS protocolId, source, source_hash AS sourceHash, version, "
          + "saved_by AS savedBy, created_at AS createdAt FROM iot_protocol_revision "
          + "WHERE protocol_id = #{protocolId} ORDER BY created_at DESC, id DESC")
  List<IoTProtocolRevision> list(@Param("protocolId") String protocolId);

  @Delete("DELETE FROM iot_protocol_revision WHERE id = #{id} AND protocol_id = #{protocolId}")
  int delete(@Param("id") String id, @Param("protocolId") String protocolId);

  @Delete("DELETE FROM iot_protocol_revision WHERE protocol_id = #{protocolId}")
  int deleteAll(@Param("protocolId") String protocolId);
}
