package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.entity.Movie;
import com.movierec.entity.Rating;
import com.movierec.entity.User;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.MovieService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieMapper movieMapper;
    private final RatingMapper ratingMapper;
    private final UserMapper userMapper;

    public MovieServiceImpl(MovieMapper movieMapper, RatingMapper ratingMapper, UserMapper userMapper) {
        this.movieMapper = movieMapper;
        this.ratingMapper = ratingMapper;
        this.userMapper = userMapper;
    }

    @Override
    public IPage<MovieSearchVO> search(String keyword, int page, int size) {
        Page<Movie> moviePage = new Page<>(page, size);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Movie::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Movie::getDoubanRating);

        IPage<Movie> result = movieMapper.selectPage(moviePage, wrapper);

        return result.convert(this::toSearchVO);
    }

    @Override
    public MovieDetailVO getDetail(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            return null;
        }

        MovieDetailVO vo = new MovieDetailVO();
        vo.setId(movie.getId());
        vo.setDoubanId(movie.getDoubanId());
        vo.setTitle(movie.getTitle());
        vo.setDirector(movie.getDirector());
        vo.setActors(movie.getActors());
        vo.setGenre(movie.getGenre());
        vo.setReleaseDate(movie.getReleaseDate());
        vo.setRuntime(movie.getRuntime());
        vo.setSummary(movie.getSummary());
        vo.setPosterUrl(movie.getPosterUrl());
        vo.setDoubanRating(movie.getDoubanRating());
        vo.setAvgRating(movie.getAvgRating());

        LambdaQueryWrapper<Rating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(Rating::getMovieId, id);
        Long count = ratingMapper.selectCount(ratingWrapper);
        vo.setRatingCount(count != null ? count.intValue() : 0);

        return vo;
    }

    @Override
    public List<Movie> getHotMovies(int limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Movie::getDoubanRating)
               .orderByDesc(Movie::getDoubanRating)
               .last("LIMIT " + limit);
        return movieMapper.selectList(wrapper);
    }

    @Override
    public List<Movie> getColdStartMovies(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPreferences() == null || user.getPreferences().trim().isEmpty()) {
            LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Movie::getDoubanRating)
                   .orderByDesc(Movie::getDoubanRating)
                   .last("LIMIT 20");
            return movieMapper.selectList(wrapper);
        }

        String[] prefs = user.getPreferences().split(",");
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Movie::getDoubanRating);
        wrapper.and(w -> {
            for (int i = 0; i < prefs.length; i++) {
                String genre = prefs[i].trim();
                if (genre.isEmpty()) continue;
                if (i == 0) {
                    w.like(Movie::getGenre, genre);
                } else {
                    w.or().like(Movie::getGenre, genre);
                }
            }
        });
        wrapper.orderByDesc(Movie::getDoubanRating).last("LIMIT 20");

        return movieMapper.selectList(wrapper);
    }

    @Override
    public IPage<MovieSearchVO> listByGenre(int page, int size, String genre) {
        Page<Movie> moviePage = new Page<>(page, size);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Movie::getDoubanRating);
        if (genre != null && !genre.trim().isEmpty() && !"全部".equals(genre)) {
            wrapper.like(Movie::getGenre, genre.trim());
        }
        wrapper.orderByDesc(Movie::getDoubanRating);
        IPage<Movie> result = movieMapper.selectPage(moviePage, wrapper);
        return result.convert(this::toSearchVO);
    }

    private MovieSearchVO toSearchVO(Movie movie) {
        MovieSearchVO vo = new MovieSearchVO();
        vo.setId(movie.getId());
        vo.setTitle(movie.getTitle());
        vo.setPosterUrl(movie.getPosterUrl());
        vo.setAvgRating(movie.getAvgRating());
        vo.setDoubanRating(movie.getDoubanRating());
        vo.setGenre(movie.getGenre());
        if (movie.getReleaseDate() != null) {
            vo.setReleaseYear(String.valueOf(movie.getReleaseDate().getYear()));
        }
        return vo;
    }
}
