package com.movierec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movierec.entity.SecurityAuditEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecurityAuditEventMapper extends BaseMapper<SecurityAuditEvent> {
}
