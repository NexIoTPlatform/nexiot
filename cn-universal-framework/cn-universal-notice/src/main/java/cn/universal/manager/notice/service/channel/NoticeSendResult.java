package cn.universal.manager.notice.service.channel;

public class NoticeSendResult {

  private java.util.List<String> messageIds = new java.util.ArrayList<>();
  public java.util.List<String> getMessageIds() { return messageIds; }
  public void setMessageIds(java.util.List<String> messageIds) { this.messageIds = messageIds; }

  private boolean success;
  private String receivers;
  private String content;
  private String errorMessage;

  public NoticeSendResult() {}

  public NoticeSendResult(boolean success, String receivers, String content, String errorMessage) {
    this.success = success;
    this.receivers = receivers;
    this.content = content;
    this.errorMessage = errorMessage;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getReceivers() {
    return receivers;
  }

  public void setReceivers(String receivers) {
    this.receivers = receivers;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean success;
    private String receivers;
    private String content;
    private String errorMessage;

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder receivers(String receivers) {
      this.receivers = receivers;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public NoticeSendResult build() {
      return new NoticeSendResult(success, receivers, content, errorMessage);
    }
  }
}
