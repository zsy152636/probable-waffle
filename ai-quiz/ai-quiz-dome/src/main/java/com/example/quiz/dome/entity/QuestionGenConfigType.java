package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_gen_config_type")
public class QuestionGenConfigType {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long configId;
    private String questionType;
    private Integer count;
    private Integer sort;
}
