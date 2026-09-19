package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.dto.FavoriteVO;
import com.movierec.entity.Favorite;
import com.movierec.entity.Movie;
import com.movierec.exception.ConflictException;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.mapper.FavoriteMapper;
import com.movierec.mapper.MovieMapper;
import com.movierec.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MovieMapper movieMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, MovieMapper movieMapper) {
        this.favoriteMapper = favoriteMapper;
        this.movieMapper = movieMapper;
    }

    @Override
    public Favorite addFavorite(Long userId, Long movieId) {
        if (movieMapper.selectById(movieId) == null) {
            throw new ResourceNotFoundException("电影不存在");
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getMovieId, movieId);
        if (favoriteMapper.selectOne(wrapper) != null) {
            throw new ConflictException("已收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMovieId(movieId);
        favoriteMapper.insert(favorite);
        return favorite;
    }

    @Override
    public void removeFavorite(Long userId, Long movieId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getMovieId, movieId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    public List<FavoriteVO> getUserFavorites(Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreateTime);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);

        return favorites.stream().map(fav -> {
            FavoriteVO vo = new FavoriteVO();
            vo.setFavoriteId(fav.getId());
            vo.setMovieId(fav.getMovieId());
            vo.setCreateTime(fav.getCreateTime());

            Movie movie = movieMapper.selectById(fav.getMovieId());
            if (movie != null) {
                vo.setMovieTitle(movie.getTitle());
                vo.setPosterUrl(movie.getPosterUrl());
                vo.setAvgRating(movie.getAvgRating());
                vo.setGenre(movie.getGenre());
                vo.setDoubanRating(movie.getDoubanRating());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
