package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_paper_answer")
public class TestPaperAnswer {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long testPaperId;
    private Long questionId;
    private String questionType;
    private String correctAnswer;
    private Integer score;
    private Integer sort;
}
