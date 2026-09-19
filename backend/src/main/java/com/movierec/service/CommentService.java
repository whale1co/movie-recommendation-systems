package com.movierec.service;

import com.movierec.dto.CommentVO;
import com.movierec.dto.MyCommentVO;

import java.util.List;
import java.util.Map;

public interface CommentService {

    CommentVO addComment(Long userId, Long movieId, String content);

    List<CommentVO> getCommentsByMovieId(Long movieId, Long currentUserId);

    Map<String, Object> toggleLike(Long userId, Long commentId);

    int getTotalLikesByUserId(Long userId);

    List<MyCommentVO> getMyComments(Long userId);
}
