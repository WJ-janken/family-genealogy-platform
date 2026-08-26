package com.family.genealogy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.genealogy.entity.MemberClosure;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MemberClosureMapper extends BaseMapper<MemberClosure> {

    /**
     * 为新节点插入闭包记录
     * 包括：自身到自身(depth=0) + 所有祖先到自身的路径
     */
    @Insert("INSERT INTO member_closure (ancestor_id, descendant_id, depth) " +
            "SELECT ancestor_id, #{descendantId}, depth + 1 " +
            "FROM member_closure WHERE descendant_id = #{parentId} " +
            "UNION ALL SELECT #{descendantId}, #{descendantId}, 0")
    void insertClosureForNewNode(@Param("descendantId") Long descendantId, @Param("parentId") Long parentId);

    /**
     * 插入根节点的闭包记录（仅自身到自身）
     */
    @Insert("INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES (#{id}, #{id}, 0)")
    void insertClosureForRoot(@Param("id") Long id);

    /**
     * 删除某节点及其所有后代的闭包记录
     */
    @Delete("DELETE FROM member_closure WHERE descendant_id IN " +
            "(SELECT descendant_id FROM (SELECT descendant_id FROM member_closure WHERE ancestor_id = #{nodeId}) tmp)")
    void deleteClosureForSubtree(@Param("nodeId") Long nodeId);

    /**
     * 查询某节点的所有后代ID
     */
    @Select("SELECT descendant_id FROM member_closure WHERE ancestor_id = #{ancestorId} AND depth > 0")
    List<Long> selectDescendantIds(@Param("ancestorId") Long ancestorId);

    /**
     * 清空闭包表（覆盖导入时使用）
     */
    @Delete("DELETE FROM member_closure")
    void deleteAll();
}
