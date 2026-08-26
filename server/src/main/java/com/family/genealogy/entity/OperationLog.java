package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private String module;

    private String action;

    private String targetType;

    private Long targetId;

    private String description;

    private String requestIp;

    private String requestData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
