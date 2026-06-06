package com.example.quiz.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.quiz.sys.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("""
        <script>
        SELECT DISTINCT m.* FROM sys_menu m
        INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
        WHERE rm.role_id IN
        <foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
        ORDER BY m.sort
        </script>
    """)
    List<Menu> findMenusByRoleIds(List<Long> roleIds);
}
