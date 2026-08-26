package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字辈诗/字辈歌实体类
 * 用于管理不同姓氏分支的字辈序列及其释义
 */
@Data
@TableName("sys_generation_poetry")
public class GenerationPoetry {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 姓氏
     */
    private String surname;

    /**
     * 分支名称
     */
    private String branchName;

    /**
     * 字辈诗/字辈歌标题
     */
    private String title;

    /**
     * 字辈序列（按世代顺序排列）
     */
    private String generationSequence;

    /**
     * 字辈释义
     */
    private String interpretation;

    /**
     * 起始世代
     */
    private Integer startGeneration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 备注
     */
    private String remark;
}