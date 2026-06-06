package com.example.quiz.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quiz.sys.common.PageResult;
import com.example.quiz.sys.common.Result;
import com.example.quiz.sys.entity.User;
import com.example.quiz.sys.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public Result<PageResult<User>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword) {
        Page<User> p = userService.page(page, pageSize, keyword);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public Result<?> add(@RequestBody User user) {
        userService.add(user);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<?> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.update(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody User user) {
        userService.updateStatus(id, user.getStatus());
        return Result.ok();
    }
}
