package cn.universal.miniapp.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 小程序用户设备绑定实体
 *
 * <p>用于描述在指定小程序应用(appId)下，某个小程序用户(userId)被授权可见/可操作的设备(iotId)。
 */
@Table(name = "mp_user_device")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpUserDevice implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 小程序用户ID */
  @Column(name = "user_id")
  private Long userId;

  /** IoT设备唯一标识 */
  @Column(name = "iot_id")
  private String iotId;

  /** 创建时间 */
  @Column(name = "create_time")
  private Date createTime;
}
