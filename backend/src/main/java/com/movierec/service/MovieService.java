package com.movierec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.entity.Movie;

import java.util.List;

public interface MovieService {

    IPage<MovieSearchVO> search(String keyword, int page, int size);

    MovieDetailVO getDetail(Long id);

    List<Movie> getHotMovies(int limit);

    List<Movie> getColdStartMovies(Long userId);

    IPage<MovieSearchVO> listByGenre(int page, int size, String genre);
}
