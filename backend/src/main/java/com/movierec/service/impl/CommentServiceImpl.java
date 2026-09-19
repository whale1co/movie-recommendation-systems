package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.dto.CommentVO;
import com.movierec.dto.MyCommentVO;
import com.movierec.entity.Comment;
import com.movierec.entity.CommentLike;
import com.movierec.entity.Movie;
import com.movierec.entity.User;
import com.movierec.mapper.CommentLikeMapper;
import com.movierec.mapper.CommentMapper;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Override
    public CommentVO addComment(Long userId, Long movieId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setMovieId(movieId);
        comment.setContent(content);
        comment.setLikeCount(0);
        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getId());
        vo.setMovieId(movieId);
        vo.setContent(content);
        vo.setUsername(user != null ? user.getUsername() : "未知用户");
        vo.setLikeCount(0);
        vo.setLiked(false);
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    @Override
    public List<CommentVO> getCommentsByMovieId(Long movieId, Long currentUserId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getMovieId, movieId)
               .orderByDesc(Comment::getCreateTime);
        List<Comment> comments = commentMapper.selectList(wrapper);

        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> userNameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        // 查询当前用户点赞了哪些评论
        Set<Long> likedCommentIds = new HashSet<>();
        if (currentUserId != null) {
            List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
            LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(CommentLike::getUserId, currentUserId)
                       .in(CommentLike::getCommentId, commentIds);
            List<CommentLike> likes = commentLikeMapper.selectList(likeWrapper);
            likedCommentIds = likes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }

        Set<Long> finalLikedCommentIds = likedCommentIds;
        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setCommentId(c.getId());
            vo.setMovieId(c.getMovieId());
            vo.setContent(c.getContent());
            vo.setUsername(userNameMap.getOrDefault(c.getUserId(), "未知用户"));
            vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
            vo.setLiked(finalLikedCommentIds.contains(c.getId()));
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> toggleLike(Long userId, Long commentId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
               .eq(CommentLike::getCommentId, commentId);
        CommentLike existing = commentLikeMapper.selectOne(wrapper);

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        boolean liked;
        if (existing != null) {
            // 取消点赞
            commentLikeMapper.deleteById(existing.getId());
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1));
            liked = false;
        } else {
            // 点赞
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeMapper.insert(like);
            comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);
            liked = true;
        }
        commentMapper.updateById(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", comment.getLikeCount());
        return result;
    }

    @Override
    public int getTotalLikesByUserId(Long userId) {
        // 查找该用户的所有评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId);
        List<Comment> comments = commentMapper.selectList(wrapper);
        if (comments.isEmpty()) {
            return 0;
        }
        return comments.stream()
                .mapToInt(c -> c.getLikeCount() != null ? c.getLikeCount() : 0)
                .sum();
    }

    @Override
    public List<MyCommentVO> getMyComments(Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId)
               .orderByDesc(Comment::getCreateTime);
        List<Comment> comments = commentMapper.selectList(wrapper);

        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> movieIds = comments.stream().map(Comment::getMovieId).collect(Collectors.toSet());
        List<Movie> movies = movieMapper.selectBatchIds(movieIds);
        Map<Long, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));

        return comments.stream().map(c -> {
            MyCommentVO vo = new MyCommentVO();
            vo.setCommentId(c.getId());
            vo.setMovieId(c.getMovieId());
            Movie movie = movieMap.get(c.getMovieId());
            vo.setMovieTitle(movie != null ? movie.getTitle() : "未知电影");
            vo.setPosterUrl(movie != null ? movie.getPosterUrl() : "");
            vo.setContent(c.getContent());
            vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }
}
