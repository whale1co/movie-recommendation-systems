package com.movierec.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.movierec.ai.model.MovieIntent;
import com.movierec.dto.response.AiAdvisorHistoryDetail;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.dto.response.AiMovieRecommendation;
import com.movierec.entity.AiAdvisorHistory;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.mapper.AiAdvisorHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAdvisorHistoryServiceTest {
    @Mock AiAdvisorHistoryMapper historyMapper;
    private ObjectMapper objectMapper;
    private AiAdvisorHistoryService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        service = new AiAdvisorHistoryService(historyMapper, objectMapper);
    }

    @Test
    void savesCompleteRecommendationSnapshotForCurrentUser() {
        AiAdvisorResponse response = response();

        service.saveQuietly(7L, "两小时内的喜剧", response);

        ArgumentCaptor<AiAdvisorHistory> captor = ArgumentCaptor.forClass(AiAdvisorHistory.class);
        verify(historyMapper).insert(captor.capture());
        AiAdvisorHistory saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals(1, saved.getRecommendationCount());
        assertEquals(42L, read(saved.getResultSnapshot()).recommendations().get(0).movieId());
        assertEquals("符合片长和类型", read(saved.getResultSnapshot()).recommendations().get(0).reason());
    }

    @Test
    void readsOnlyOwnedHistoryAndKeepsSnapshotContent() throws Exception {
        AiAdvisorHistory history = history(11L, 7L, response());
        when(historyMapper.selectOne(any(Wrapper.class))).thenReturn(history);

        AiAdvisorHistoryDetail detail = service.get(7L, 11L);

        assertEquals(11L, detail.id());
        assertEquals(42L, detail.result().recommendations().get(0).movieId());
        assertEquals(List.of("喜剧", "120分钟以内"),
                detail.result().recommendations().get(0).matchedCriteria());
    }

    @Test
    void otherUsersCannotReadOrDeleteHistory() {
        when(historyMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(historyMapper.delete(any(Wrapper.class))).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> service.get(8L, 11L));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(8L, 11L));
    }

    private AiAdvisorResponse response() {
        AiMovieRecommendation movie = new AiMovieRecommendation(42L, "测试电影", null, "喜剧",
                108, null, new BigDecimal("8.6"), "符合片长和类型",
                List.of("喜剧", "120分钟以内"), "/movie/42");
        return new AiAdvisorResponse("推荐如下", MovieIntent.empty(), List.of(movie), true, false, "fake");
    }

    private AiAdvisorHistory history(Long id, Long userId, AiAdvisorResponse response) throws Exception {
        AiAdvisorHistory history = new AiAdvisorHistory();
        history.setId(id);
        history.setUserId(userId);
        history.setQuestion("两小时内的喜剧");
        history.setResultSnapshot(objectMapper.writeValueAsString(response));
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    private AiAdvisorResponse read(String json) {
        try {
            return objectMapper.readValue(json, AiAdvisorResponse.class);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
