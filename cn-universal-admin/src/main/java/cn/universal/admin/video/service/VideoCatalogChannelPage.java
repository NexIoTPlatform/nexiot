package cn.universal.admin.video.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Channel pages deliberately share only the catalog pagination envelope. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCatalogChannelPage {
  private List<VideoCatalogChannel> rows;
  private long total;
  private int pageNum;
  private int pageSize;
  private boolean hasMore;
}
