package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.entity.Movie;
import com.movierec.entity.Rating;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.service.RecommendService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final int K_NEIGHBORS = 30;
    private static final int MIN_RATINGS = 5;

    private final RatingMapper ratingMapper;
    private final MovieMapper movieMapper;

    public RecommendServiceImpl(RatingMapper ratingMapper, MovieMapper movieMapper) {
        this.ratingMapper = ratingMapper;
        this.movieMapper = movieMapper;
    }

    @Override
    public List<Long> getUserCFRecommendations(Long userId, int topN) {
        Map<Long, Map<Long, Double>> userMovieRatings = loadAllRatings();

        Map<Long, Double> targetRatings = userMovieRatings.get(userId);
        if (targetRatings == null || targetRatings.size() < MIN_RATINGS) {
            return Collections.emptyList();
        }

        double targetAvg = calculateAverage(targetRatings);

        Map<Long, Double> similarityMap = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userMovieRatings.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) continue;

            Map<Long, Double> otherRatings = entry.getValue();
            double sim = pearsonSimilarity(targetRatings, targetAvg, otherRatings);
            if (sim > 0) {
                similarityMap.put(otherUserId, sim);
            }
        }

        if (similarityMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<Long, Double>> sortedNeighbors = similarityMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(K_NEIGHBORS)
                .collect(Collectors.toList());

        Set<Long> ratedMovies = targetRatings.keySet();
        Map<Long, Double> predictedScores = new HashMap<>();

        for (Map.Entry<Long, Double> neighbor : sortedNeighbors) {
            Long neighborId = neighbor.getKey();
            double sim = neighbor.getValue();
            Map<Long, Double> neighborRatings = userMovieRatings.get(neighborId);
            double neighborAvg = calculateAverage(neighborRatings);

            for (Map.Entry<Long, Double> movieRating : neighborRatings.entrySet()) {
                Long movieId = movieRating.getKey();
                if (ratedMovies.contains(movieId)) continue;

                double neighborDeviation = movieRating.getValue() - neighborAvg;
                predictedScores.merge(movieId, sim * neighborDeviation, Double::sum);
            }
        }

        Map<Long, Double> simSumMap = new HashMap<>();
        for (Map.Entry<Long, Double> neighbor : sortedNeighbors) {
            Long neighborId = neighbor.getKey();
            double sim = neighbor.getValue();
            Map<Long, Double> neighborRatings = userMovieRatings.get(neighborId);

            for (Long movieId : neighborRatings.keySet()) {
                if (ratedMovies.contains(movieId)) continue;
                simSumMap.merge(movieId, Math.abs(sim), Double::sum);
            }
        }

        Map<Long, Double> finalPredictions = new HashMap<>();
        for (Map.Entry<Long, Double> entry : predictedScores.entrySet()) {
            Long movieId = entry.getKey();
            double weightedSum = entry.getValue();
            double simSum = simSumMap.getOrDefault(movieId, 0.0);
            if (simSum > 0) {
                double predictedRating = targetAvg + weightedSum / simSum;
                finalPredictions.put(movieId, predictedRating);
            }
        }

        return finalPredictions.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> getRecommendations(Long userId, int topN) {
        List<Long> movieIds = getUserCFRecommendations(userId, topN);
        if (movieIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Movie> movies = new ArrayList<>();
        for (Long movieId : movieIds) {
            Movie movie = movieMapper.selectById(movieId);
            if (movie != null) {
                movies.add(movie);
            }
        }
        return movies;
    }

    private Map<Long, Map<Long, Double>> loadAllRatings() {
        List<Rating> allRatings = ratingMapper.selectList(null);
        Map<Long, Map<Long, Double>> userMovieRatings = new HashMap<>();
        for (Rating rating : allRatings) {
            userMovieRatings
                    .computeIfAbsent(rating.getUserId(), k -> new HashMap<>())
                    .put(rating.getMovieId(), rating.getScore());
        }
        return userMovieRatings;
    }

    private double pearsonSimilarity(Map<Long, Double> ratings1, double avg1,
                                     Map<Long, Double> ratings2) {
        Set<Long> commonMovies = new HashSet<>(ratings1.keySet());
        commonMovies.retainAll(ratings2.keySet());

        if (commonMovies.size() < 2) {
            return 0.0;
        }

        double avg2 = calculateAverage(ratings2);

        double numerator = 0.0;
        double sumSq1 = 0.0;
        double sumSq2 = 0.0;

        for (Long movieId : commonMovies) {
            double dev1 = ratings1.get(movieId) - avg1;
            double dev2 = ratings2.get(movieId) - avg2;
            numerator += dev1 * dev2;
            sumSq1 += dev1 * dev1;
            sumSq2 += dev2 * dev2;
        }

        double denominator = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    private double calculateAverage(Map<Long, Double> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}
