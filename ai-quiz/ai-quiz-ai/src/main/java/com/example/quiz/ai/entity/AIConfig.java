package com.example.quiz.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_config")
public class AIConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String providerName;
    private String apiUrl;
    private String apiToken;
    private String modelName;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
