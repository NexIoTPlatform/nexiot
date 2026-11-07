package cn.universal.persistence.message;

import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.BaseUPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("CustomMessageAdapter")
public class CustomMessageAdapter extends AbstractIoTMessageAdapter {

  @Override
  public String name() {
    return "tencent";
  }

  @Override
  public String formatUpMessage(BaseUPRequest upRequest) {
    return null;
  }

  @Override
  public String formatDownMessage(BaseDownRequest downRequest) {
    return null;
  }
}
