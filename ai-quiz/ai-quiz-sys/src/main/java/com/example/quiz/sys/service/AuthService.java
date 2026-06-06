package com.example.quiz.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.quiz.sys.common.BusinessException;
import com.example.quiz.sys.entity.Menu;
import com.example.quiz.sys.entity.User;
import com.example.quiz.sys.mapper.UserMapper;
import com.example.quiz.sys.security.JwtTokenProvider;
import com.example.quiz.sys.security.UserDetailsServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtTokenProvider jwtTokenProvider,
                       UserDetailsServiceImpl userDetailsService,
                       StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expiresIn", 7200);
        return result;
    }

    public void logout(String token) {
        String jti = jwtTokenProvider.getJti(token);
        long remaining = jwtTokenProvider.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set("token:blacklist:" + jti, "1", Duration.ofMillis(remaining));
        }
    }

    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        List<String> permissions = userDetailsService.getUserPermissions(userId);
        List<Menu> menus = userDetailsService.getUserMenus(userId);
        List<Map<String, Object>> menuTree = buildMenuTree(menus, 0L);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId().toString());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("avatar", null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", userInfo);
        result.put("roles", List.of("ADMIN"));
        result.put("permissions", permissions);
        result.put("menus", menuTree);
        return result;
    }

    private List<Map<String, Object>> buildMenuTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId) && m.getVisible() == 1)
                .sorted(Comparator.comparingInt(Menu::getSort))
                .map(m -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", m.getId());
                    node.put("parentId", m.getParentId());
                    node.put("name", m.getMenuName());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("icon", m.getIcon());
                    List<Map<String, Object>> children = buildMenuTree(menus, m.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }
}
