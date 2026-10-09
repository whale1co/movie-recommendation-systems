package com.movierec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.dto.AdminMovieVO;
import com.movierec.dto.request.AdminMovieUpdateRequest;
import com.movierec.common.PageResponse;
import com.movierec.entity.Movie;

import java.util.List;

public interface MovieService {

    IPage<MovieSearchVO> search(String keyword, int page, int size);

    MovieDetailVO getDetail(Long id);

    List<Movie> getHotMovies(int limit);

    List<Movie> getColdStartMovies(Long userId);

    IPage<MovieSearchVO> listByGenre(int page, int size, String genre);

    PageResponse<AdminMovieVO> listAdminMovies(long current, long size, String keyword, String genre, Integer year, String status);

    AdminMovieVO getAdminMovie(Long id);

    AdminMovieVO updateAdminMovie(Long id, AdminMovieUpdateRequest request);

    void deleteAdminMovie(Long id);
}
