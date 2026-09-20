package com.movierec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movierec.common.PageResponse;
import com.movierec.dto.SecurityAuditEventVO;
import com.movierec.entity.SecurityAuditEvent;
import com.movierec.mapper.SecurityAuditEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class SecurityAuditService {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);
    private final SecurityAuditEventMapper mapper;

    public SecurityAuditService(SecurityAuditEventMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String eventType, String outcome, Long actorUserId, String subject, String metadata) {
        try {
            SecurityAuditEvent event = new SecurityAuditEvent();
            event.setEventType(limit(eventType, 60));
            event.setOutcome(limit(outcome, 20));
            event.setActorUserId(actorUserId);
            event.setSubject(limit(subject, 100));
            event.setMetadata(limit(metadata, 500));
            event.setRequestId(limit(MDC.get("requestId"), 100));
            event.setIpAddress(limit(currentIp(), 45));
            mapper.insert(event);
        } catch (RuntimeException ex) {
            log.error("Unable to persist security audit event type={}", eventType, ex);
        }
    }

    public PageResponse<SecurityAuditEventVO> list(long current, long size) {
        Page<SecurityAuditEvent> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SecurityAuditEvent>().orderByDesc(SecurityAuditEvent::getCreatedAt));
        List<SecurityAuditEventVO> records = page.getRecords().stream()
                .map(event -> new SecurityAuditEventVO(event.getId(), event.getActorUserId(),
                        event.getEventType(), event.getOutcome(), event.getSubject(), event.getIpAddress(),
                        event.getRequestId(), event.getMetadata(), event.getCreatedAt()))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private String currentIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest().getRemoteAddr();
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() <= max ? sanitized : sanitized.substring(0, max);
    }
}
