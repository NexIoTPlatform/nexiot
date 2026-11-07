package cn.universal.admin.video.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCatalogPage {
  private List<VideoCatalogDevice> rows;
  private long total;
  private int pageNum;
  private int pageSize;
  private boolean hasMore;
}
