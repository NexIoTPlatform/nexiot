package cn.universal.core.api.crypto.bean;

import java.io.InputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

/** 解密后的请求体输入流。 */
@Getter
@RequiredArgsConstructor
public class DecryptHttpInputMessage implements HttpInputMessage {

  private final InputStream body;
  private final HttpHeaders headers;
}
