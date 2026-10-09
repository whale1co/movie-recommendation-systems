package com.movierec.service;

import com.movierec.dto.CommentVO;
import com.movierec.dto.MyCommentVO;
import com.movierec.dto.response.LikeResponse;

import java.util.List;

public interface CommentService {

    CommentVO addComment(Long userId, Long movieId, String content);

    List<CommentVO> getCommentsByMovieId(Long movieId, Long currentUserId);

    LikeResponse toggleLike(Long userId, Long commentId);

    int getTotalLikesByUserId(Long userId);

    List<MyCommentVO> getMyComments(Long userId);
}
