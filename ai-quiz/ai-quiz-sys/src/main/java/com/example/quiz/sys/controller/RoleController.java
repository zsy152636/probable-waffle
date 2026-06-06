package com.example.quiz.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import com.example.quiz.sys.entity.Menu;
import com.example.quiz.sys.entity.Role;
import com.example.quiz.sys.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public Result<PageResult<Role>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Page<Role> p = roleService.page(page, pageSize);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/all")
    public Result<List<Role>> all() {
        return Result.ok(roleService.all());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public Result<?> add(@RequestBody Role role) {
        roleService.add(role);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<?> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.update(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<Menu>> getMenus(@PathVariable Long id) {
        return Result.ok(roleService.getMenusByRoleId(id));
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('role:assignPerm')")
    public Result<?> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignMenus(id, body.get("menuIds"));
        return Result.ok();
    }
}
