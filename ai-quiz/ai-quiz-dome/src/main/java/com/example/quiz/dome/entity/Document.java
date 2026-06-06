package com.example.quiz.dome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_document")
public class Document {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String docNo;
    private String docName;
    private String docType;
    private String filePath;
    private Long fileSize;
    private String contentText;
    private Integer questionCount;
    private LocalDateTime questionGenTime;
    private Long uploadUserId;
    private String uploadUserName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
