package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("quiz_result")
public class QuizResult {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long testPaperId;
    private Long documentId;
    private Long userId;
    private Integer totalScore;
    private Integer userScore;
    private Integer correctCount;
    private Integer totalCount;
    private String answersJson;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer durationSeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
