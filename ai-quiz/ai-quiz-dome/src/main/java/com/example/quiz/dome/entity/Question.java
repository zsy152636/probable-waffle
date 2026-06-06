package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_question")
public class Question {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long documentId;
    private String questionType;
    private String questionTitle;
    private String passage;
    private String analysis;
    private Integer difficulty;
    private Integer score;
    private Integer sort;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
