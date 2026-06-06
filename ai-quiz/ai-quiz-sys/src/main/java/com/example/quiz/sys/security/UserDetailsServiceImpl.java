package com.example.quiz.sys.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.quiz.sys.entity.Menu;
import com.example.quiz.sys.entity.User;
import com.example.quiz.sys.mapper.MenuMapper;
import com.example.quiz.sys.mapper.RoleMapper;
import com.example.quiz.sys.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public UserDetailsServiceImpl(UserMapper userMapper, RoleMapper roleMapper, MenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用");
        }

        List<String> permissions = getUserPermissions(user.getId());

        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    public List<String> getUserPermissions(Long userId) {
        List<Long> roleIds = roleMapper.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return List.of();
        List<Menu> menus = menuMapper.findMenusByRoleIds(roleIds);
        return menus.stream()
                .map(Menu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Menu> getUserMenus(Long userId) {
        List<Long> roleIds = roleMapper.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return List.of();
        return menuMapper.findMenusByRoleIds(roleIds);
    }
}
