package com.movierec.controller;

import com.movierec.entity.Rating;
import com.movierec.entity.User;
import com.movierec.service.RatingService;
import com.movierec.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public Result<Rating> addRating(@AuthenticationPrincipal User currentUser,
                                    @RequestBody Map<String, Object> body) {
        Long movieId = Long.valueOf(body.get("movieId").toString());
        Double score = Double.valueOf(body.get("score").toString());

        try {
            Rating rating = ratingService.addRating(currentUser.getId(), movieId, score);
            return Result.success("评分成功", rating);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Rating> updateRating(@AuthenticationPrincipal User currentUser,
                                       @PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        Double score = Double.valueOf(body.get("score").toString());

        try {
            Rating rating = ratingService.updateRating(id, currentUser.getId(), score);
            return Result.success("修改成功", rating);
        } catch (RuntimeException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRating(@AuthenticationPrincipal User currentUser,
                                     @PathVariable Long id) {
        try {
            ratingService.deleteRating(id, currentUser.getId());
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(403, e.getMessage());
        }
    }
}
