package com.movierec.controller;

import com.movierec.dto.CommentVO;
import com.movierec.dto.MyCommentVO;
import com.movierec.entity.User;
import com.movierec.service.CommentService;
import com.movierec.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Result<CommentVO> addComment(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Long movieId = Long.valueOf(body.get("movieId").toString());
        String content = body.get("content").toString();
        if (content == null || content.trim().isEmpty()) {
            return Result.error(400, "评论内容不能为空");
        }
        CommentVO vo = commentService.addComment(currentUser.getId(), movieId, content.trim());
        return Result.success("评论成功", vo);
    }

    @GetMapping
    public Result<List<CommentVO>> getComments(
            @RequestParam Long movieId,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        List<CommentVO> list = commentService.getCommentsByMovieId(movieId, currentUserId);
        return Result.success("获取成功", list);
    }

    @PostMapping("/{commentId}/like")
    public Result<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long commentId) {
        Map<String, Object> result = commentService.toggleLike(currentUser.getId(), commentId);
        return Result.success("操作成功", result);
    }

    @GetMapping("/total-likes")
    public Result<Integer> getTotalLikes(@AuthenticationPrincipal User currentUser) {
        int total = commentService.getTotalLikesByUserId(currentUser.getId());
        return Result.success("查询成功", total);
    }

    @GetMapping("/my-comments")
    public Result<List<MyCommentVO>> getMyComments(@AuthenticationPrincipal User currentUser) {
        List<MyCommentVO> list = commentService.getMyComments(currentUser.getId());
        return Result.success("查询成功", list);
    }
}
