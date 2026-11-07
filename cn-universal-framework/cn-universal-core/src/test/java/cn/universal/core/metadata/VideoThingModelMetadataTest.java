package cn.universal.core.metadata;

import cn.hutool.json.JSONUtil;
import cn.universal.core.metadata.types.ArrayType;
import cn.universal.core.metadata.types.ObjectType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class VideoThingModelMetadataTest {

  @Test
  public void persistenceVideoTemplateShouldDeclareFunctionOutputs() throws Exception {
    assertVideoFunctionOutputs(
        Files.readString(
            findFromWorkingDirectory("cn-universal-persistence/video_camera_unified_thing_model.json")));
  }

  @Test
  public void adminDefaultVideoTemplateShouldDeclareFunctionOutputs() throws Exception {
    assertVideoFunctionOutputs(readAdminDefaultVideoMetadata());
  }

  private void assertVideoFunctionOutputs(String metadataJson) {
    DeviceMetadata metadata = new DeviceMetadata(JSONUtil.parseObj(metadataJson));

    assertObjectOutput(metadata, "cameraLiveStream", "rtsp", "hls", "urls");
    assertObjectOutput(metadata, "cameraPlayback", "hls", "downloadUrl", "records", "responseData");
    assertObjectOutput(metadata, "cameraSnapshot", "snapshotUrl", "snapshotData", "responseData");

    AbstractFunctionMetadata queryRecords = requireFunction(metadata, "queryRecords");
    ValueType output = queryRecords.getOutput();
    Assert.assertEquals(ArrayType.ID, output.getId());
    Assert.assertTrue(output instanceof ArrayType);

    ValueType elementType = ((ArrayType) output).getElementType();
    Assert.assertNotNull(elementType);
    Assert.assertEquals(ObjectType.ID, elementType.getId());
    Assert.assertTrue(elementType instanceof ObjectType);
    assertProperties(
        (ObjectType) elementType, "startTime", "endTime", "recordType", "playbackUrl", "raw");
  }

  private void assertObjectOutput(DeviceMetadata metadata, String functionId, String... expectedProperties) {
    AbstractFunctionMetadata function = requireFunction(metadata, functionId);
    ValueType output = function.getOutput();
    Assert.assertEquals(ObjectType.ID, output.getId());
    Assert.assertTrue(output instanceof ObjectType);
    assertProperties((ObjectType) output, expectedProperties);
  }

  private void assertProperties(ObjectType objectType, String... expectedProperties) {
    Set<String> propertyIds =
        objectType.getProperties().stream()
            .map(AbstractPropertyMetadata::getId)
            .collect(Collectors.toSet());
    for (String property : expectedProperties) {
      Assert.assertTrue("missing output property: " + property, propertyIds.contains(property));
    }
  }

  private AbstractFunctionMetadata requireFunction(DeviceMetadata metadata, String functionId) {
    AbstractFunctionMetadata function = metadata.getFunctionOrNull(functionId);
    Assert.assertNotNull("missing function: " + functionId, function);
    return function;
  }

  private String readAdminDefaultVideoMetadata() throws Exception {
    Path source =
        findFromWorkingDirectory(
            "cn-universal-admin/src/main/java/cn/universal/admin/video/service/impl/"
                + "VideoPlatformProductServiceImpl.java");
    String text = Files.readString(source);
    String marker = "private String metadata =";
    int markerIndex = text.indexOf(marker);
    Assert.assertTrue("missing metadata field", markerIndex >= 0);

    int blockStart = text.indexOf("\"\"\"", markerIndex);
    Assert.assertTrue("missing metadata text block start", blockStart >= 0);
    int contentStart = text.indexOf('\n', blockStart);
    Assert.assertTrue("missing metadata text block content", contentStart >= 0);
    contentStart++;

    int blockEnd = text.indexOf("\"\"\";", contentStart);
    Assert.assertTrue("missing metadata text block end", blockEnd >= 0);
    return text.substring(contentStart, blockEnd);
  }

  private Path findFromWorkingDirectory(String relativePath) {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new AssertionError("Cannot find " + relativePath);
  }
}
