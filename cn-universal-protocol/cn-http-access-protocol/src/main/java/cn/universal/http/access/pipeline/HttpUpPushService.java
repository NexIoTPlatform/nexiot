package cn.universal.http.access.pipeline;

import cn.universal.core.service.IUP;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HttpUpPushService extends IoTUPPushAdapter<BaseUPRequest> implements IUP {

  public void push(List<BaseUPRequest> requests) {
    doUp(requests);
  }

  @Override
  public String name() {
    return "http";
  }

  @Override
  public void debugAsyncUP(String debugMsg) {
    // HTTP debugging uses the management run endpoint so route context is preserved.
  }
}
