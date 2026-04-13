package com.yishou.liuyao.session.dto;

/** 追问请求 */
public class MessageRequest {

    private String content;

    public MessageRequest() {}
    public MessageRequest(String content) { this.content = content; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
