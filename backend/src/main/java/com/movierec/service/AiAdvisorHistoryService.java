package com.movierec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.common.PageResponse;
import com.movierec.dto.response.AiAdvisorHistoryDetail;
import com.movierec.dto.response.AiAdvisorHistorySummary;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.entity.AiAdvisorHistory;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.mapper.AiAdvisorHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiAdvisorHistoryService {
    private static final Logger log = LoggerFactory.getLogger(AiAdvisorHistoryService.class);

    private final AiAdvisorHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    public AiAdvisorHistoryService(AiAdvisorHistoryMapper historyMapper, ObjectMapper objectMapper) {
        this.historyMapper = historyMapper;
        this.objectMapper = objectMapper;
    }

    public void saveQuietly(Long userId, String question, AiAdvisorResponse result) {
        try {
            AiAdvisorHistory history = new AiAdvisorHistory();
            history.setUserId(userId);
            history.setQuestion(question);
            history.setAnswer(result.answer());
            history.setResultSnapshot(objectMapper.writeValueAsString(result));
            history.setRecommendationCount(result.recommendations().size());
            history.setAiGenerated(result.aiGenerated());
            history.setDegraded(result.degraded());
            history.setProvider(result.provider());
            historyMapper.insert(history);
        } catch (Exception ex) {
            log.warn("Unable to persist AI advisor history for userId={}: {}",
                    userId, ex.getClass().getSimpleName());
        }
    }

    public PageResponse<AiAdvisorHistorySummary> list(Long userId, long page, long size) {
        Page<AiAdvisorHistory> result = historyMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AiAdvisorHistory>()
                        .eq(AiAdvisorHistory::getUserId, userId)
                        .orderByDesc(AiAdvisorHistory::getCreatedAt)
                        .orderByDesc(AiAdvisorHistory::getId));
        return new PageResponse<>(result.getRecords().stream().map(this::toSummary).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AiAdvisorHistoryDetail get(Long userId, Long id) {
        AiAdvisorHistory history = findOwned(userId, id);
        try {
            AiAdvisorResponse snapshot = objectMapper.readValue(history.getResultSnapshot(), AiAdvisorResponse.class);
            return new AiAdvisorHistoryDetail(history.getId(), history.getQuestion(), snapshot, history.getCreatedAt());
        } catch (JsonProcessingException ex) {
            log.warn("Unable to read AI advisor history id={} for userId={}", id, userId);
            throw new ResourceNotFoundException("历史记录不可用");
        }
    }

    public void delete(Long userId, Long id) {
        int deleted = historyMapper.delete(new LambdaQueryWrapper<AiAdvisorHistory>()
                .eq(AiAdvisorHistory::getId, id)
                .eq(AiAdvisorHistory::getUserId, userId));
        if (deleted == 0) throw new ResourceNotFoundException("历史记录不存在");
    }

    private AiAdvisorHistory findOwned(Long userId, Long id) {
        AiAdvisorHistory history = historyMapper.selectOne(new LambdaQueryWrapper<AiAdvisorHistory>()
                .eq(AiAdvisorHistory::getId, id)
                .eq(AiAdvisorHistory::getUserId, userId));
        if (history == null) throw new ResourceNotFoundException("历史记录不存在");
        return history;
    }

    private AiAdvisorHistorySummary toSummary(AiAdvisorHistory history) {
        String answer = history.getAnswer() == null ? "" : history.getAnswer();
        String preview = answer.length() <= 160 ? answer : answer.substring(0, 160) + "...";
        return new AiAdvisorHistorySummary(history.getId(), history.getQuestion(), preview,
                history.getRecommendationCount() == null ? 0 : history.getRecommendationCount(),
                Boolean.TRUE.equals(history.getAiGenerated()), Boolean.TRUE.equals(history.getDegraded()),
                history.getCreatedAt());
    }
}
