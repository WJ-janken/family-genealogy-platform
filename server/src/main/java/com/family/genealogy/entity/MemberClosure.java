package com.family.genealogy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员闭包表实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_closure")
public class MemberClosure {

    private Long ancestorId;

    private Long descendantId;

    private Integer depth;
}
