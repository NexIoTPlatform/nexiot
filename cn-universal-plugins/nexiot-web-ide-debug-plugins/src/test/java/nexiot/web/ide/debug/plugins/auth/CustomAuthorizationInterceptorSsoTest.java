package nexiot.web.ide.debug.plugins.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.MagicConfiguration;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.interceptor.Authorization;
import nexiot.web.ide.debug.plugins.core.model.Group;
import nexiot.web.ide.debug.plugins.core.model.MagicEntity;
import nexiot.web.ide.debug.plugins.core.service.MagicResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CustomAuthorizationInterceptorSsoTest {
  final MagicSsoSessionService sessions = new MagicSsoSessionService();
  final CustomAuthorizationInterceptor auth = new CustomAuthorizationInterceptor();
  final MagicResourceService resources = mock(MagicResourceService.class);
  MagicUser user;

  @BeforeEach
  void setup() throws Exception {
    ReflectionTestUtils.setField(auth, "ssoSessions", sessions);
    new MagicConfiguration().setMagicResourceService(resources);
    user = sessions.issue("user-a", "tenant-a", false, Instant.now().plusSeconds(900), () -> {});
  }

  @Test
  void scopedUsersCannotReachGlobalManagementApis() {
    assertFalse(auth.allowRequest(user, "MagicBackupController", "backup"));
    assertFalse(auth.allowRequest(user, "MagicDataSourceController", "test"));
    for (String method : new String[] {"reload", "upload", "push"})
      assertFalse(auth.allowRequest(user, "MagicWorkbenchController", method));
    assertTrue(auth.allowRequest(user, "MagicWorkbenchController", "user"));
    assertTrue(auth.allowRequest(user, "MagicResourceController", "resources"));
  }

  @Test
  void forgedResourceOwnerCannotBypassStoredOwnership() {
    MagicEntity persisted = new MagicEntity();
    persisted.setCreateBy("other-user");
    when(resources.file("other-file")).thenReturn(persisted);
    MagicEntity forged = new MagicEntity();
    forged.setId("other-file");
    forged.setCreateBy("user-a");
    forged.setGroupId(Constants.ROOT_ID);
    assertFalse(auth.allowVisit(user, null, Authorization.VIEW, forged));
    assertFalse(auth.allowVisit(user, null, Authorization.SAVE, forged));
  }

  @Test
  void filesCanOnlyBeCreatedInOwnedFolders() {
    Group other = new Group();
    other.setCreateBy("other-user");
    when(resources.getGroup("other-folder")).thenReturn(other);
    MagicEntity file = new MagicEntity();
    file.setGroupId("other-folder");
    assertFalse(auth.allowVisit(user, null, Authorization.SAVE, file));
    other.setCreateBy("user-a");
    assertTrue(auth.allowVisit(user, null, Authorization.SAVE, file));
  }

  @Test
  void ownerlessLegacyFilesAreNotAutomaticallySharedWithSsoUsers() {
    MagicEntity file = new MagicEntity();
    file.setId("legacy-file");
    when(resources.file("legacy-file")).thenReturn(file);
    assertFalse(auth.allowVisit(user, null, Authorization.VIEW, file));
  }

  @Test
  void movingFoldersToAnotherOwnersFolderIsRejected() {
    Group source = new Group();
    source.setCreateBy("user-a");
    Group other = new Group();
    other.setCreateBy("other-user");
    when(resources.getGroup("source")).thenReturn(source);
    when(resources.getGroup("target")).thenReturn(other);
    Group move = new Group();
    move.setId("source");
    move.setParentId("target");
    assertFalse(auth.allowVisit(user, null, Authorization.DELETE, move));
  }

  @Test
  void folderOwnershipCannotBeReassignedBySubmittedJson() {
    Group original = new Group();
    original.setCreateBy("user-a");
    when(resources.getGroup("owned-folder")).thenReturn(original);
    Group submitted = new Group();
    submitted.setId("owned-folder");
    submitted.setParentId(Constants.ROOT_ID);
    submitted.setCreateBy("other-user");
    assertTrue(auth.allowVisit(user, null, Authorization.SAVE, submitted));
    assertEquals("user-a", submitted.getCreateBy());
  }

  @Test
  void administratorKeepsGlobalWorkbenchAccess() throws Exception {
    MagicUser admin = sessions.issue("admin", "0", true, Instant.now().plusSeconds(900), () -> {});
    assertTrue(auth.allowRequest(admin, "MagicBackupController", "backup"));
  }
}
