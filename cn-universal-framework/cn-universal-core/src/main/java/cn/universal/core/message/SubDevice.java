package cn.universal.core.message;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 网关子设备信息
 *
 * @author NexIoT
 * @version 1.0
 * @since 2025/10/16 22:10
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class SubDevice implements Serializable {
  private static final long serialVersionUID = 1L;
  private String productKey;

  /** modbus从站地址 */
  private String slaveAddress;

  /** opcua */
  private String nodeId;

  /** S7的DB块号; */
  private String dbNumber;

  /** S7的偏移地址; */
  private String offset;
}
