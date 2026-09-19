package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.CommentVO;
import com.movierec.dto.MyCommentVO;
import com.movierec.dto.request.CreateCommentRequest;
import com.movierec.dto.response.LikeResponse;
import com.movierec.entity.User;
import com.movierec.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "评论")
@Validated
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "发布评论")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentVO>> addComment(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentVO comment = commentService.addComment(
                currentUser.getId(), request.movieId(), request.content().trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("评论成功", comment));
    }

    @Operation(summary = "查看电影评论")
    @GetMapping
    public ApiResponse<List<CommentVO>> getComments(
            @Positive(message = "电影ID必须为正数") @RequestParam Long movieId,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        return ApiResponse.success("获取成功",
                commentService.getCommentsByMovieId(movieId, currentUserId));
    }

    @Operation(summary = "切换评论点赞状态")
    @PostMapping("/{commentId}/like")
    public ApiResponse<LikeResponse> toggleLike(
            @AuthenticationPrincipal User currentUser,
            @Positive(message = "评论ID必须为正数") @PathVariable Long commentId) {
        return ApiResponse.success("操作成功",
                commentService.toggleLike(currentUser.getId(), commentId));
    }

    @Operation(summary = "查看本人评论获赞总数")
    @GetMapping("/total-likes")
    public ApiResponse<Integer> getTotalLikes(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("查询成功",
                commentService.getTotalLikesByUserId(currentUser.getId()));
    }

    @Operation(summary = "查看本人评论")
    @GetMapping("/mine")
    public ApiResponse<List<MyCommentVO>> getMyComments(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("查询成功", commentService.getMyComments(currentUser.getId()));
    }
}
