package com.movierec.ai.service;

import com.movierec.exception.BusinessException;
import com.movierec.service.SecurityAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiInputGuard {
    private static final List<String> INJECTION_MARKERS = List.of(
            "忽略之前", "忽略以上", "忽略系统", "系统提示词", "开发者消息", "泄露提示",
            "ignore previous", "ignore all", "system prompt", "developer message", "reveal prompt",
            "jailbreak", "越狱");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(sk-[a-z0-9_-]{16,}|bearer\\s+[a-z0-9._-]{16,}|password\\s*[:=]|密码\\s*[:：=])");

    private final SecurityAuditService auditService;

    public AiInputGuard(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    public void validate(Long userId, String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        if (INJECTION_MARKERS.stream().anyMatch(normalized::contains)) {
            reject(userId, "prompt_injection", "问题包含不安全的提示指令");
        }
        if (SECRET_PATTERN.matcher(question).find()) {
            reject(userId, "sensitive_data", "请勿在问题中提交密码、令牌或密钥");
        }
        long controlCharacters = question.chars()
                .filter(ch -> Character.isISOControl(ch) && !Character.isWhitespace(ch)).count();
        if (controlCharacters > 0) {
            reject(userId, "control_characters", "问题包含不支持的控制字符");
        }
    }

    private void reject(Long userId, String reason, String message) {
        auditService.record("AI_INPUT", "DENIED", userId, null, "reason=" + reason);
        throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
