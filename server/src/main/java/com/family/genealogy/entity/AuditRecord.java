package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录实体
 */
@Data
@TableName("audit_record")
public class AuditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submitterId;

    private String targetType;

    private Long targetId;

    private String action;

    /** JSON 格式存储 */
    private String beforeData;

    /** JSON 格式存储 */
    private String afterData;

    private String status;

    private Long reviewerId;

    private String reviewComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String submitterName;

    @TableField(exist = false)
    private String reviewerName;

    @TableField(exist = false)
    private String targetName;
}
