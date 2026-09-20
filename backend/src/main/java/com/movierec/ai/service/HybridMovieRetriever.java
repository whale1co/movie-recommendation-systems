package com.movierec.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movierec.ai.config.AiProperties;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;
import com.movierec.entity.Movie;
import com.movierec.mapper.MovieMapper;
import com.movierec.service.RecommendService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HybridMovieRetriever {
    private final MovieMapper movieMapper;
    private final RecommendService recommendService;
    private final MovieDocumentFactory documentFactory;
    private final AiProperties properties;

    public HybridMovieRetriever(MovieMapper movieMapper, RecommendService recommendService,
                                MovieDocumentFactory documentFactory, AiProperties properties) {
        this.movieMapper = movieMapper;
        this.recommendService = recommendService;
        this.documentFactory = documentFactory;
        this.properties = properties;
    }

    public List<RetrievedMovie> retrieve(Long userId, String question, MovieIntent intent) {
        LambdaQueryWrapper<Movie> query = new LambdaQueryWrapper<>();
        if (intent.maxRuntime() != null) query.le(Movie::getRuntime, intent.maxRuntime());
        if (intent.minRating() != null) query.ge(Movie::getDoubanRating, intent.minRating());
        if (intent.minYear() != null) query.ge(Movie::getReleaseDate, intent.minYear() + "-01-01");
        if (intent.maxYear() != null) query.le(Movie::getReleaseDate, intent.maxYear() + "-12-31");
        for (String excluded : intent.excludedGenres()) query.notLike(Movie::getGenre, excluded);
        if (!intent.genres().isEmpty()) {
            query.and(nested -> {
                for (int i = 0; i < intent.genres().size(); i++) {
                    if (i == 0) nested.like(Movie::getGenre, intent.genres().get(i));
                    else nested.or().like(Movie::getGenre, intent.genres().get(i));
                }
            });
        }
        query.orderByDesc(Movie::getDoubanRating).orderByDesc(Movie::getRatingCount);
        List<Movie> movies = movieMapper.selectPage(new Page<>(1, 200, false), query).getRecords();

        List<Long> cfIds = recommendService.getUserCFRecommendations(userId, 100);
        Map<Long, Double> cfScores = new HashMap<>();
        for (int i = 0; i < cfIds.size(); i++) cfScores.put(cfIds.get(i), 1.0 - (double) i / Math.max(1, cfIds.size()));

        Set<String> terms = terms(question, intent);
        return movies.stream().map(movie -> score(movie, intent, terms, cfScores.getOrDefault(movie.getId(), 0.0)))
                .sorted((left, right) -> Double.compare(right.finalScore(), left.finalScore()))
                .limit(Math.max(3, Math.min(properties.getRetrievalLimit(), 20)))
                .toList();
    }

    private RetrievedMovie score(Movie movie, MovieIntent intent, Set<String> terms, double cfScore) {
        String document = documentFactory.create(movie).toLowerCase(Locale.ROOT);
        long hits = terms.stream().filter(document::contains).count();
        double semantic = terms.isEmpty() ? 0.5 : Math.min(1.0, (double) hits / terms.size());
        List<String> matches = new ArrayList<>();
        if (intent.maxRuntime() != null && movie.getRuntime() != null) matches.add("片长不超过" + intent.maxRuntime() + "分钟");
        if (intent.minRating() != null) matches.add("评分达到" + intent.minRating());
        if (!intent.genres().isEmpty()) matches.add("类型匹配");
        if (hits > 0) matches.add("主题关键词匹配");
        double constraint = intent.genres().isEmpty() && intent.maxRuntime() == null && intent.minRating() == null
                && intent.minYear() == null && intent.maxYear() == null ? 0.5 : 1.0;
        double rating = rating(movie).doubleValue() / 10.0;
        double popularity = Math.min(1.0, Math.log10(Math.max(1, movie.getRatingCount()) + 1) / 6.0);
        double finalScore = 0.40 * semantic + 0.25 * constraint + 0.20 * cfScore + 0.10 * rating + 0.05 * popularity;
        return new RetrievedMovie(movie, finalScore, semantic, constraint, cfScore, matches);
    }

    private Set<String> terms(String question, MovieIntent intent) {
        Set<String> terms = new HashSet<>();
        terms.addAll(intent.genres());
        terms.addAll(intent.keywords());
        for (String token : question.toLowerCase(Locale.ROOT).split("[\\s,，。！？、;；:：]+")) {
            if (token.length() >= 2 && token.length() <= 20) terms.add(token);
        }
        terms.removeAll(intent.excludedGenres());
        return terms;
    }

    private BigDecimal rating(Movie movie) {
        if (movie.getDoubanRating() != null) return movie.getDoubanRating();
        return movie.getAvgRating() != null ? movie.getAvgRating() : BigDecimal.ZERO;
    }
}
