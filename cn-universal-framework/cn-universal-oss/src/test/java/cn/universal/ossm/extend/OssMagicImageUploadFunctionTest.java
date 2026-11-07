package cn.universal.ossm.extend;

import static org.junit.Assert.assertEquals;

import cn.universal.core.engine.MagicScript;
import cn.universal.core.engine.MagicScriptContext;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.ossm.util.VideoSnapshotOssHelper;
import java.lang.reflect.Proxy;
import org.junit.Test;

public class OssMagicImageUploadFunctionTest {

  @Test
  public void returnsUploadedUrlForImageBytes() {
    OssImageUploadFunction function = new OssImageUploadFunction(helper(false));

    assertEquals("https://oss.example/image.jpg", function.uploadImage(new byte[] {1, 2, 3}));
    assertEquals("https://oss.example/image.jpg", function.upload_image(new byte[] {1, 2, 3}));
  }

  @Test
  public void returnsEmptyStringWhenUploadFails() {
    OssImageUploadFunction function = new OssImageUploadFunction(helper(true));

    assertEquals("", function.uploadImage(new byte[] {1, 2, 3}));
  }

  @Test
  public void isCallableFromMagicScript() {
    OssImageUploadFunction function = new OssImageUploadFunction(helper(false));
    JavaReflection.registerFunction(function);

    MagicScript script = MagicScript.create("return upload_image(image);", null);
    MagicScriptContext context = new MagicScriptContext().set("image", new byte[] {1, 2, 3});

    assertEquals("https://oss.example/image.jpg", script.execute(context));
  }

  private VideoSnapshotOssHelper helper(boolean fail) {
    ISysOssService ossService =
        (ISysOssService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {ISysOssService.class},
                (proxy, method, args) -> {
                  if ("uploadStream".equals(method.getName())) {
                    if (fail) {
                      throw new IllegalStateException("upload failed");
                    }
                    return new SysOss().setUrl("https://oss.example/image.jpg");
                  }
                  if (method.getReturnType().isPrimitive()) {
                    return false;
                  }
                  return null;
                });
    return new VideoSnapshotOssHelper(ossService);
  }
}
