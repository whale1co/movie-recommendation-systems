package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.dto.AdminMovieVO;
import com.movierec.dto.request.AdminMovieUpdateRequest;
import com.movierec.common.PageResponse;
import com.movierec.exception.ResourceNotFoundException;
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
        wrapper.eq(Movie::getStatus, "ACTIVE");
        wrapper.orderByDesc(Movie::getDoubanRating);

        IPage<Movie> result = movieMapper.selectPage(moviePage, wrapper);

        return result.convert(this::toSearchVO);
    }

    @Override
    public MovieDetailVO getDetail(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null || "DELETED".equalsIgnoreCase(movie.getStatus())) {
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
        vo.setStatus(movie.getStatus() == null ? "ACTIVE" : movie.getStatus());

        return vo;
    }

    @Override
    public List<Movie> getHotMovies(int limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Movie::getStatus, "ACTIVE").isNotNull(Movie::getDoubanRating)
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
        wrapper.eq(Movie::getStatus, "ACTIVE").isNotNull(Movie::getDoubanRating);
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
        wrapper.eq(Movie::getStatus, "ACTIVE").isNotNull(Movie::getDoubanRating);
        if (genre != null && !genre.trim().isEmpty() && !"全部".equals(genre)) {
            wrapper.like(Movie::getGenre, genre.trim());
        }
        wrapper.orderByDesc(Movie::getDoubanRating);
        IPage<Movie> result = movieMapper.selectPage(moviePage, wrapper);
        return result.convert(this::toSearchVO);
    }

    @Override
    public PageResponse<AdminMovieVO> listAdminMovies(long current, long size, String keyword, String genre, Integer year, String status) {
        Page<Movie> page = new Page<>(current, size);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        String normalizedStatus = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
        String normalizedGenre = genre == null ? null : genre.trim();
        wrapper.eq(Movie::getStatus, normalizedStatus)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(Movie::getTitle, keyword.trim())
                        .or().like(Movie::getDirector, keyword.trim()).or().like(Movie::getDoubanId, keyword.trim()))
                .like(normalizedGenre != null && !normalizedGenre.isBlank(), Movie::getGenre, normalizedGenre)
                .apply(year != null, "YEAR(release_date) = {0}", year)
                .orderByDesc(Movie::getCreateTime);
        Page<Movie> result = movieMapper.selectPage(page, wrapper);
        List<AdminMovieVO> records = result.getRecords().stream().map(this::toAdminVO).toList();
        return new PageResponse<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminMovieVO getAdminMovie(Long id) {
        return toAdminVO(requireMovie(id));
    }

    @Override
    public AdminMovieVO updateAdminMovie(Long id, AdminMovieUpdateRequest request) {
        if (!request.hasChanges()) throw new IllegalArgumentException("至少提供一个需要修改的字段");
        Movie movie = requireMovie(id);
        if (request.title() != null) movie.setTitle(request.title().trim());
        if (request.director() != null) movie.setDirector(request.director());
        if (request.actors() != null) movie.setActors(request.actors());
        if (request.genre() != null) movie.setGenre(request.genre());
        if (request.releaseDate() != null) movie.setReleaseDate(request.releaseDate());
        if (request.runtime() != null) movie.setRuntime(request.runtime());
        if (request.summary() != null) movie.setSummary(request.summary());
        if (request.posterUrl() != null) movie.setPosterUrl(request.posterUrl());
        if (request.doubanRating() != null) movie.setDoubanRating(request.doubanRating());
        if (request.status() != null) movie.setStatus(request.status());
        movieMapper.updateById(movie);
        return toAdminVO(movie);
    }

    @Override
    public void deleteAdminMovie(Long id) {
        Movie movie = requireMovie(id);
        movie.setStatus("DELETED");
        movieMapper.updateById(movie);
    }

    private Movie requireMovie(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) throw new ResourceNotFoundException("电影不存在");
        return movie;
    }

    private AdminMovieVO toAdminVO(Movie movie) {
        AdminMovieVO vo = new AdminMovieVO();
        vo.setId(movie.getId()); vo.setDoubanId(movie.getDoubanId()); vo.setTitle(movie.getTitle());
        vo.setDirector(movie.getDirector()); vo.setActors(movie.getActors()); vo.setGenre(movie.getGenre());
        vo.setReleaseDate(movie.getReleaseDate()); vo.setRuntime(movie.getRuntime()); vo.setSummary(movie.getSummary());
        vo.setPosterUrl(movie.getPosterUrl()); vo.setDoubanRating(movie.getDoubanRating()); vo.setAvgRating(movie.getAvgRating());
        vo.setRatingCount(movie.getRatingCount()); vo.setStatus(movie.getStatus() == null ? "ACTIVE" : movie.getStatus());
        vo.setCreateTime(movie.getCreateTime());
        return vo;
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
