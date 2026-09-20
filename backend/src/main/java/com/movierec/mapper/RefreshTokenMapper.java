package com.movierec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movierec.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    @Select("SELECT COUNT(*) FROM refresh_token WHERE session_id = #{sessionId} AND revoked_at IS NULL AND expires_at > #{now}")
    long countActiveSession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    @Update("UPDATE refresh_token SET revoked_at = #{now} WHERE session_id = #{sessionId} AND revoked_at IS NULL")
    int revokeSession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    @Update("UPDATE refresh_token SET revoked_at = #{now}, last_used_at = #{now} WHERE id = #{id} AND revoked_at IS NULL AND expires_at > #{now}")
    int revokeForRotation(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("UPDATE refresh_token SET revoked_at = #{now} WHERE user_id = #{userId} AND revoked_at IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
