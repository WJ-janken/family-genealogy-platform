package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任职履历实体
 */
@Data
@TableName("career_record")
public class CareerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String position;

    private String organization;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
