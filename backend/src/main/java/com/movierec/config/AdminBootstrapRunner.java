package com.movierec.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.entity.User;
import com.movierec.mapper.UserAuthorizationMapper;
import com.movierec.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Creates or promotes one explicitly configured bootstrap administrator at startup. */
@Configuration
public class AdminBootstrapRunner {

    @Bean
    CommandLineRunner bootstrapAdmin(UserMapper userMapper,
                                     UserAuthorizationMapper authorizationMapper,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${admin.bootstrap.enabled:false}") boolean enabled,
                                     @Value("${admin.bootstrap.username:}") String username,
                                     @Value("${admin.bootstrap.password:}") String password) {
        return args -> {
            if (!enabled) return;
            bootstrap(userMapper, authorizationMapper, passwordEncoder, username, password);
        };
    }

    @Transactional
    void bootstrap(UserMapper userMapper, UserAuthorizationMapper authorizationMapper,
                   PasswordEncoder passwordEncoder, String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 8) {
            throw new IllegalStateException("管理员初始化需要非空 ADMIN_BOOTSTRAP_USERNAME 和至少 8 位 ADMIN_BOOTSTRAP_PASSWORD");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null) {
            user = new User();
            user.setUsername(username.trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("ADMIN");
            user.setStatus("ACTIVE");
            userMapper.insert(user);
        } else {
            user.setRole("ADMIN");
            user.setStatus("ACTIVE");
            user.setPassword(passwordEncoder.encode(password));
            userMapper.updateById(user);
        }
        Long roleId = authorizationMapper.findRoleId("ADMIN");
        if (roleId == null) throw new IllegalStateException("ADMIN 角色不存在，请先执行数据库迁移");
        authorizationMapper.deleteUserRoles(user.getId());
        authorizationMapper.addUserRole(user.getId(), roleId);
    }
}