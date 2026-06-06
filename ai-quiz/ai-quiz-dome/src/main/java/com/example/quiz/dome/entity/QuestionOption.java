package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("doc_question_option")
public class QuestionOption {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionId;
    private String optionLabel;
    private String optionText;
    private Integer isCorrect;
    private Integer sort;
}
