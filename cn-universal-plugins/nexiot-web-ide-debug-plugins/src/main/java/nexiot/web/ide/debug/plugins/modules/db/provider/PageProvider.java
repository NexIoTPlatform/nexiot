package nexiot.web.ide.debug.plugins.modules.db.provider;

import cn.universal.core.engine.runtime.RuntimeContext;
import nexiot.web.ide.debug.plugins.modules.db.model.Page;

/**
 * 分页对象提取接口
 *
 * @author mxd
 */
public interface PageProvider {

  /**
   * 从请求中获取分页对象
   *
   * @param context 脚本上下文
   * @return 返回分页对象
   */
  public Page getPage(RuntimeContext context);
}
