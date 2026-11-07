package cn.universal.persistence.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 仅包含有限原始样本的归约输入，不把整条设备消息传回应用。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyTrendSampleVO {
  private int windowIndex;
  private String content;
}
