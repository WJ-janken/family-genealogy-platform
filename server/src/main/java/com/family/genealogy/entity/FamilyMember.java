package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 族谱成员实体
 */
@Data
@TableName("family_member")
public class FamilyMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 谱名（辈分名，如"郑志泉"） */
    private String name;

    /** 俗名/书名（户口名，如"郑二狗"） */
    private String aliasName;

    /** 辈分字（如"志"） */
    private String generationChar;

    private String gender;

    private Integer generation;

    private String branch;

    private Long parentId;

    private Long spouseId;

    private LocalDate birthDate;

    private LocalDate deathDate;

    private String birthPlace;

    private String biography;

    private String avatarUrl;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;

    /** 非数据库字段：子节点数量 */
    @TableField(exist = false)
    private Integer childrenCount;
}
