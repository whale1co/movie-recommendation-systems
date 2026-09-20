package com.movierec.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAuthorizationMapper {
    @Select("SELECT DISTINCT r.code FROM `role` r JOIN `user_role` ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> findRoleCodes(Long userId);

    @Select("SELECT DISTINCT p.code FROM `permission` p JOIN `role_permission` rp ON rp.permission_id = p.id JOIN `user_role` ur ON ur.role_id = rp.role_id WHERE ur.user_id = #{userId}")
    List<String> findPermissionCodes(Long userId);

    @Select("SELECT id FROM `role` WHERE code = #{roleCode}")
    Long findRoleId(String roleCode);

    @Delete("DELETE FROM `user_role` WHERE user_id = #{userId}")
    int deleteUserRoles(Long userId);

    @Insert("INSERT INTO `user_role` (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int addUserRole(Long userId, Long roleId);
}