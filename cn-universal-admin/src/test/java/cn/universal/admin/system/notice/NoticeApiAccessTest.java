package cn.universal.admin.system.notice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class NoticeApiAccessTest {
  private final OauthClientDetailsMapper clients = mock(OauthClientDetailsMapper.class);
  private final IoTUserService users = mock(IoTUserService.class);
  private final NoticeApiAccess access = new NoticeApiAccess();
  NoticeApiAccessTest() {
    ReflectionTestUtils.setField(access, "noticeAccess", new cn.universal.manager.notice.service.NoticeAccess(users, clients, mock(cn.universal.security.support.DeptDataPermissionSupport.class)));
  }
  @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
  private void authenticate(Jwt.Builder jwt) {
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.header("alg", "RS256").build(), java.util.List.of()));
  }
  @Test void resolvesRegisteredClientOwnerWithoutRequiringApplicationRow() {
    IoTUser owner = new IoTUser(); owner.setUnionId("owner");
    OAuth2ClientDetails client = new OAuth2ClientDetails(); client.setIotUnionId("owner");
    when(clients.findByClientId("client-1")).thenReturn(client);
    when(users.selectUserByUnionId("owner")).thenReturn(owner);
    authenticate(Jwt.withTokenValue("test").subject("client-1").claim("clientId", "client-1").claim("unionId", "owner"));
    assertSame(owner, access.noticeApiUser());
  }
  @Test void userSubjectIsNotReinterpretedAsAnApplication() {
    IoTUser user = new IoTUser(); user.setUnionId("user");
    when(users.selectUserByUnionId("user")).thenReturn(user);
    authenticate(Jwt.withTokenValue("test").subject("user"));
    assertSame(user, access.noticeApiUser());
    verifyNoInteractions(clients);
  }
  @Test void rejectsChangedClientOwner() {
    OAuth2ClientDetails client = new OAuth2ClientDetails(); client.setIotUnionId("new-owner");
    when(clients.findByClientId("client-1")).thenReturn(client);
    authenticate(Jwt.withTokenValue("test").subject("client-1").claim("clientId", "client-1").claim("unionId", "old-owner"));
    assertThrows(ResponseStatusException.class, access::noticeApiUser);
  }
  @Test void rejectsMismatchedOrUnknownClient() {
    authenticate(Jwt.withTokenValue("test").subject("client-1").claim("clientId", "different"));
    assertThrows(ResponseStatusException.class, access::noticeApiUser);
    authenticate(Jwt.withTokenValue("test").subject("missing"));
    assertThrows(ResponseStatusException.class, access::noticeApiUser);
  }
}
