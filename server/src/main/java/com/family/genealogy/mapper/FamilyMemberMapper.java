package com.family.genealogy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.genealogy.entity.FamilyMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {

    /**
     * 查询某节点的所有后代（通过闭包表）
     */
    @Select("SELECT m.*, (SELECT COUNT(*) FROM family_member c WHERE c.parent_id = m.id AND c.is_deleted = 0) as children_count " +
            "FROM family_member m " +
            "INNER JOIN member_closure mc ON m.id = mc.descendant_id " +
            "WHERE mc.ancestor_id = #{ancestorId} AND mc.depth <= #{maxDepth} AND m.is_deleted = 0 " +
            "ORDER BY m.generation, m.sort_order")
    List<FamilyMember> selectDescendants(@Param("ancestorId") Long ancestorId, @Param("maxDepth") int maxDepth);

    /**
     * 查询某节点的直接子节点
     */
    @Select("SELECT m.*, (SELECT COUNT(*) FROM family_member c WHERE c.parent_id = m.id AND c.is_deleted = 0) as children_count " +
            "FROM family_member m " +
            "WHERE m.parent_id = #{parentId} AND m.is_deleted = 0 " +
            "ORDER BY m.sort_order")
    List<FamilyMember> selectChildren(@Param("parentId") Long parentId);

    /**
     * 查询某节点的祖先链
     */
    @Select("SELECT m.* FROM family_member m " +
            "INNER JOIN member_closure mc ON m.id = mc.ancestor_id " +
            "WHERE mc.descendant_id = #{descendantId} AND mc.depth > 0 AND m.is_deleted = 0 " +
            "ORDER BY mc.depth DESC")
    List<FamilyMember> selectAncestors(@Param("descendantId") Long descendantId);

    /**
     * 获取所有世代列表
     */
    @Select("SELECT DISTINCT generation FROM family_member WHERE is_deleted = 0 ORDER BY generation")
    List<Integer> selectAllGenerations();

    /**
     * 获取所有房支列表
     */
    @Select("SELECT DISTINCT branch FROM family_member WHERE is_deleted = 0 AND branch != '' ORDER BY branch")
    List<String> selectAllBranches();
}
