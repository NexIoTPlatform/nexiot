package cn.universal.dm.video;

import static org.junit.jupiter.api.Assertions.*;
import cn.universal.common.exception.BaseException;
import java.util.*;
import org.junit.jupiter.api.Test;

class VideoOperationRegistryTest {
  @Test void readersCannotUseConfigOrInternalActionsAndOldEditorsKeepPublicRights() {
    assertTrue(VideoOperationRegistry.requirePublic("deviceInfo").allowed(Set.of("video:platform:view")));
    assertFalse(VideoOperationRegistry.requirePublic("deviceConfigApply").allowed(Set.of("video:platform:view")));
    assertTrue(VideoOperationRegistry.requirePublic("presetGoto").allowed(Set.of("video:platform:edit")));
    assertThrows(BaseException.class,()->VideoOperationRegistry.requirePublic("configure"));
    assertFalse(VideoOperationRegistry.find("subscribe").orElseThrow().allowed(Set.of("*:*:*")));
  }
  @Test void canonicalAliasesHaveTypedBoundedInputAndCannotInjectTransportCredentials() {
    var definition=VideoOperationRegistry.requirePublic("presetCall");assertEquals("presetGoto",definition.id());
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(definition,Map.of("deviceId","ipc","channelId","1","presetId","1")));
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(definition,Map.of("deviceId","ipc","channelId","1","zlmApiSecret","secret")));
    assertDoesNotThrow(()->VideoOperationRegistry.validate(VideoOperationRegistry.requirePublic("previewHeartbeat"),Map.of("deviceId","ipc","channelId","1","streamType","main","stream","lease","app","rtp","mediaServerId","1")));
    assertDoesNotThrow(()->VideoOperationRegistry.validate(VideoOperationRegistry.requirePublic("evidenceTimeline"),Map.of("deviceId","nvr","cursor","opaque")));
  }
  @Test void nativeEncodingSelectorsAndDeviceControlContractsAreTypedAndBounded() {
    var read=VideoOperationRegistry.requirePublic("deviceConfigRead");
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(read,Map.of("deviceId","ipc","resource","encodingNative","channelId","1")));
    for(int compressionType:List.of(1,2,3)) assertDoesNotThrow(()->VideoOperationRegistry.validate(read,Map.of("deviceId","ipc","resource","encodingNative","channelId","1","compressionType",compressionType)));
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(read,Map.of("deviceId","ipc","resource","encodingNative","channelId","1","compressionType",4)));
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(read,Map.of("deviceId","nvr","resource","alarmInput","alarmInputNo",1)));
    assertDoesNotThrow(()->VideoOperationRegistry.validate(read,Map.of("deviceId","nvr","resource","alarmInput","alarmInputNo",1,"ptzChannelNo",33)));
    var defense=VideoOperationRegistry.requirePublic("deviceDefenseControl");
    assertDoesNotThrow(()->VideoOperationRegistry.validate(defense,Map.of("deviceId","ipc","requestId","request","enabled",true,"armDelaySeconds",0)));
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(defense,Map.of("deviceId","ipc","requestId","request","enabled",true,"armDelaySeconds",301)));
    assertFalse(VideoOperationRegistry.requirePublic("deviceAlgorithmRestart").allowed(Set.of("video:device:control")));
    assertTrue(VideoOperationRegistry.requirePublic("deviceAlgorithmRestart").allowed(Set.of("video:device:maintain")));
    assertThrows(BaseException.class,()->VideoOperationRegistry.validate(defense,Map.of("deviceId","ipc","requestId","request","enabled",true,"armDelaySeconds",0,"rawXML","<anything/>")));
  }
}
