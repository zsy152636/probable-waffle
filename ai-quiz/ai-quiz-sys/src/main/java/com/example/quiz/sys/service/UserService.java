package com.example.quiz.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.sys.common.BusinessException;
import com.example.quiz.sys.entity.User;
import com.example.quiz.sys.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Page<User> page(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword).or().like(User::getRealName, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public void add(User user) {
        if (exists(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
    }

    public void update(User user) {
        User existing = userMapper.selectById(user.getId());
        if (existing == null) throw new BusinessException("用户不存在");
        if (!existing.getUsername().equals(user.getUsername()) && exists(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private boolean exists(String username) {
        return userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
