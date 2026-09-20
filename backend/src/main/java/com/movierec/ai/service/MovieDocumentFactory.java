package com.movierec.ai.service;

import com.movierec.entity.Movie;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class MovieDocumentFactory {
    public String create(Movie movie) {
        return Stream.of(movie.getTitle(), movie.getDirector(), movie.getActors(), movie.getGenre(), movie.getSummary(),
                        movie.getReleaseDate() == null ? null : movie.getReleaseDate().toString(),
                        movie.getRuntime() == null ? null : movie.getRuntime() + "分钟")
                .filter(value -> value != null && !value.isBlank())
                .reduce("站内电影ID=" + movie.getId(), (left, right) -> left + " | " + right);
    }
}
