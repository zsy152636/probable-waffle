package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("question_gen_config")
public class QuestionGenConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private Integer totalCount;
    private Integer needPassage;
    private String direction;
    private Integer sort;
    private Integer isActive;

    @TableField(exist = false)
    private List<QuestionGenConfigType> types;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
