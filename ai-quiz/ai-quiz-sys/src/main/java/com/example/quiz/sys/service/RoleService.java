package com.example.quiz.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.sys.common.BusinessException;
import com.example.quiz.sys.entity.Role;
import com.example.quiz.sys.entity.Menu;
import com.example.quiz.sys.mapper.MenuMapper;
import com.example.quiz.sys.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final JdbcTemplate jdbcTemplate;

    public RoleService(RoleMapper roleMapper, MenuMapper menuMapper, JdbcTemplate jdbcTemplate) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<Role> page(int page, int pageSize) {
        return roleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Role>().orderByDesc(Role::getCreateTime));
    }

    public List<Role> all() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByDesc(Role::getCreateTime));
    }

    public Role getById(Long id) {
        return roleMapper.selectById(id);
    }

    public void add(Role role) {
        roleMapper.insert(role);
    }

    public void update(Role role) {
        roleMapper.updateById(role);
    }

    public void delete(Long id) {
        roleMapper.deleteById(id);
    }

    public List<Menu> getMenusByRoleId(Long roleId) {
        List<Long> roleIds = List.of(roleId);
        return menuMapper.findMenusByRoleIds(roleIds);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        for (Long menuId : menuIds) {
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        }
    }
}
