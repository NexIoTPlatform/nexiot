package cn.universal.manager.notice.model;

import jakarta.persistence.Column;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** Immutable ownership snapshot; only the server may assign these fields. */
@Getter
@Setter
public abstract class NoticeOwnedResource implements Serializable {
  @Column(name = "tenant_id") private String tenantId;
  @Column(name = "dept_id") private Long deptId;
}
