package nexiot.web.ide.debug.plugins.datasource.web;

import java.sql.Connection;
import nexiot.web.ide.debug.plugins.core.config.MagicConfiguration;
import nexiot.web.ide.debug.plugins.core.model.JsonBean;
import nexiot.web.ide.debug.plugins.core.web.MagicController;
import nexiot.web.ide.debug.plugins.core.web.MagicExceptionHandler;
import nexiot.web.ide.debug.plugins.datasource.model.DataSourceInfo;
import nexiot.web.ide.debug.plugins.utils.JdbcUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

public class MagicDataSourceController extends MagicController implements MagicExceptionHandler {

  public MagicDataSourceController(MagicConfiguration configuration) {
    super(configuration);
  }

  @RequestMapping("/datasource/jdbc/test")
  @ResponseBody
  public JsonBean<String> test(@RequestBody DataSourceInfo properties) {
    try {
      Connection connection =
          JdbcUtils.getConnection(
              properties.getDriverClassName(),
              properties.getUrl(),
              properties.getUsername(),
              properties.getPassword());
      JdbcUtils.close(connection);
    } catch (Exception e) {
      return new JsonBean<>(e.getMessage());
    }
    return new JsonBean<>("ok");
  }
}
