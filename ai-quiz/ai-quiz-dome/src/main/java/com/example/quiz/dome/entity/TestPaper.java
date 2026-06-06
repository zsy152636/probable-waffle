package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("test_paper")
public class TestPaper {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String paperNo;
    private String paperName;
    private Long documentId;
    private String docNo;
    private Integer totalCount;
    private Integer singleCount;
    private Integer multiCount;
    private Integer judgeCount;
    private Integer fillCount;
    private Integer totalScore;
    private Integer status;
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
