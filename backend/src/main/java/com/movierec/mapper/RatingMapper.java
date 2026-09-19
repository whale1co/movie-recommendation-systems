package com.movierec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movierec.entity.Rating;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {
}
