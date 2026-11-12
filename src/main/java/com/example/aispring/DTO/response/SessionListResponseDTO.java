package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

/**
 * 会话列表项，字段对齐前端 consultation.vue 的会话卡片
 */
@Data
@Builder
public class SessionListResponseDTO {
    // 会话ID（对应 consultation_session.id）
    private Long id;

    // 会话标题
    private String sessionTitle;

    // 用户昵称，后台「咨询记录」列表与详情要显示
    private String userNickname;

    // 开始时间，格式化为 yyyy-MM-dd HH:mm，前端直接展示
    private String startedAt;

    // 最后一条消息时间，后台列表「时间」列使用
    private String lastMessageTime;

    // 消息条数，前端显示"共 N 条"
    private Integer messageCount;

    // 会话时长（分钟），前端显示"N 分钟"
    private Long durationMinutes;

    // 最后一条消息内容，前端显示会话预览
    private String lastMessageContent;

    // 会话状态，前端用 'TEMP' 判断是否为未落库的临时会话
    private String status;
}
