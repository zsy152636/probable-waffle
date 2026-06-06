package com.example.quiz.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.quiz.sys.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserId(Long userId);
}
