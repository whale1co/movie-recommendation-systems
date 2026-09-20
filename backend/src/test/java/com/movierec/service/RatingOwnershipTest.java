package com.movierec.service;

import com.movierec.entity.Rating;
import com.movierec.exception.ForbiddenOperationException;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.service.impl.RatingServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RatingOwnershipTest {
    @Test
    void userCannotUpdateAnotherUsersRating() {
        RatingMapper ratingMapper = mock(RatingMapper.class);
        Rating rating = rating(9L, 2L, 3L);
        when(ratingMapper.selectById(9L)).thenReturn(rating);
        RatingServiceImpl service = new RatingServiceImpl(ratingMapper, mock(MovieMapper.class));
        assertThrows(ForbiddenOperationException.class, () -> service.updateRating(9L, 1L, 4.0));
        verify(ratingMapper, never()).updateById(any());
    }

    @Test
    void userCannotDeleteAnotherUsersRating() {
        RatingMapper ratingMapper = mock(RatingMapper.class);
        Rating rating = rating(9L, 2L, 3L);
        when(ratingMapper.selectById(9L)).thenReturn(rating);
        RatingServiceImpl service = new RatingServiceImpl(ratingMapper, mock(MovieMapper.class));
        assertThrows(ForbiddenOperationException.class, () -> service.deleteRating(9L, 1L));
        verify(ratingMapper, never()).deleteById(9L);
    }

    private Rating rating(Long id, Long userId, Long movieId) {
        Rating rating = new Rating();
        rating.setId(id); rating.setUserId(userId); rating.setMovieId(movieId);
        return rating;
    }
}