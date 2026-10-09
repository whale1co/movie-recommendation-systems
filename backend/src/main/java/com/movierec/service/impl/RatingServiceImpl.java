package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.dto.RatingVO;
import com.movierec.entity.Movie;
import com.movierec.entity.Rating;
import com.movierec.exception.ConflictException;
import com.movierec.exception.ForbiddenOperationException;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.service.RatingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final MovieMapper movieMapper;

    public RatingServiceImpl(RatingMapper ratingMapper, MovieMapper movieMapper) {
        this.ratingMapper = ratingMapper;
        this.movieMapper = movieMapper;
    }

    @Override
    public Rating addRating(Long userId, Long movieId, Double score) {
        if (movieMapper.selectById(movieId) == null) {
            throw new ResourceNotFoundException("电影不存在");
        }
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getUserId, userId).eq(Rating::getMovieId, movieId);
        if (ratingMapper.selectOne(wrapper) != null) {
            throw new ConflictException("已评分，请使用修改接口");
        }

        Rating rating = new Rating();
        rating.setUserId(userId);
        rating.setMovieId(movieId);
        rating.setScore(score);
        ratingMapper.insert(rating);

        updateMovieRatingStats(movieId);
        return rating;
    }

    @Override
    public Rating updateRating(Long ratingId, Long userId, Double score) {
        Rating rating = ratingMapper.selectById(ratingId);
        if (rating == null) {
            throw new ResourceNotFoundException("评分记录不存在");
        }
        if (!rating.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("无权修改他人评分");
        }

        rating.setScore(score);
        ratingMapper.updateById(rating);

        updateMovieRatingStats(rating.getMovieId());
        return rating;
    }

    @Override
    public void deleteRating(Long ratingId, Long userId) {
        Rating rating = ratingMapper.selectById(ratingId);
        if (rating == null) {
            throw new ResourceNotFoundException("评分记录不存在");
        }
        if (!rating.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("无权删除他人评分");
        }

        ratingMapper.deleteById(ratingId);
        updateMovieRatingStats(rating.getMovieId());
    }

    @Override
    public List<RatingVO> getUserRatings(Long userId) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getUserId, userId).orderByDesc(Rating::getCreateTime);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        return ratings.stream().map(rating -> {
            RatingVO vo = new RatingVO();
            vo.setRatingId(rating.getId());
            vo.setMovieId(rating.getMovieId());
            vo.setScore(rating.getScore());
            vo.setCreateTime(rating.getCreateTime());

            Movie movie = movieMapper.selectById(rating.getMovieId());
            if (movie != null) {
                vo.setMovieTitle(movie.getTitle());
                vo.setPosterUrl(movie.getPosterUrl());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private void updateMovieRatingStats(Long movieId) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getMovieId, movieId);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        Movie movie = movieMapper.selectById(movieId);
        if (movie == null) return;

        if (ratings.isEmpty()) {
            movie.setAvgRating(null);
            movie.setRatingCount(0);
        } else {
            double sum = ratings.stream().mapToDouble(Rating::getScore).sum();
            BigDecimal avg = BigDecimal.valueOf(sum / ratings.size()).setScale(1, RoundingMode.HALF_UP);
            movie.setAvgRating(avg);
            movie.setRatingCount(ratings.size());
        }
        movieMapper.updateById(movie);
    }
}
