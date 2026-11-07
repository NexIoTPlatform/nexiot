package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

class IoTProductControllerCtwingPublicProductTest {

  @Test
  void forwardsSelectedPublicProductNameSeparatelyFromCategoryName() {
    var request =
        IoTProductController.buildCtwingPublicProductDownRequest(
            JSONUtil.parseObj(
                """
                {
                  "name": "家用烟雾报警器",
                  "productId": "GS530C-1CTDR-A",
                  "publicProductID": "99015790",
                  "companyNo": "赛特威尔电子股份有限公司",
                  "classifiedId": "2051906474821406720",
                  "classifiedName": "A"
                }
                """),
            "test");

    assertEquals("PUBPRO_ADD", request.getStr("cmd"));
    assertEquals("A", request.getStr("classifiedName"));
    assertEquals("家用烟雾报警器", request.getJSONObject("data").getStr("productName"));
    assertEquals("GS530C-1CTDR-A", request.getJSONObject("data").getStr("productId"));
    assertEquals(99015790, request.getJSONObject("data").getInt("publicProductId"));
  }
}
