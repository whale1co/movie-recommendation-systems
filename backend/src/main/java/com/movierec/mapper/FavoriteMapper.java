package com.movierec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movierec.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
