package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 签名链接生成的响应数据传输对象。
 *
 * <p>包含生成的链接及相关元数据。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedLinkResponse {

    /**
     * 签名链接路径（例如：/book/123?token=xxx&exp=xxx）。
     */
    private String link;

    /**
     * 包含基础路径的完整URL（例如：/api/public/book/123?token=xxx&exp=xxx）。
     */
    private String fullUrl;

    /**
     * 生成链接的任务ID。
     */
    private Long taskId;

    /**
     * 过期时间戳（Unix时间戳，单位：毫秒）。
     */
    private Long expiresAt;

    /**
     * ISO-8601格式的过期时间字符串。
     */
    private String expiresAtIso;

    /**
     * 链接当前是否有效。
     */
    private Boolean valid;
}
