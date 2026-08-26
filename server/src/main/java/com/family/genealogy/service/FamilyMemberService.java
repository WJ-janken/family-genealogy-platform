package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.genealogy.common.BusinessException;
import com.family.genealogy.common.PageResult;
import com.family.genealogy.entity.FamilyMember;
import com.family.genealogy.entity.MemberClosure;
import com.family.genealogy.mapper.FamilyMemberMapper;
import com.family.genealogy.mapper.MemberClosureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 族谱成员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private final FamilyMemberMapper memberMapper;
    private final MemberClosureMapper closureMapper;

    /**
     * 获取族谱树（扁平化节点列表）
     * 从指定根节点开始，加载指定深度的后代
     */
    public List<FamilyMember> getTreeNodes(Long rootId, int maxDepth) {
        if (rootId == null) {
            // 默认从始祖开始
            FamilyMember root = memberMapper.selectOne(
                    new LambdaQueryWrapper<FamilyMember>()
                            .isNull(FamilyMember::getParentId)
                            .eq(FamilyMember::getIsDeleted, 0)
                            .orderByAsc(FamilyMember::getId)
                            .last("LIMIT 1")
            );
            if (root == null) {
                return List.of();
            }
            rootId = root.getId();
        }
        return memberMapper.selectDescendants(rootId, maxDepth);
    }

    /**
     * 获取子节点（懒加载）
     */
    public List<FamilyMember> getChildren(Long parentId) {
        return memberMapper.selectChildren(parentId);
    }

    /**
     * 获取祖先链
     */
    public List<FamilyMember> getAncestors(Long memberId) {
        return memberMapper.selectAncestors(memberId);
    }

    /**
     * 获取成员详情
     */
    public FamilyMember getMemberById(Long id) {
        FamilyMember member = memberMapper.selectById(id);
        if (member == null || member.getIsDeleted() == 1) {
            throw new BusinessException("成员不存在");
        }
        return member;
    }

    /**
     * 获取成员关系信息（父母、配偶、子女）
     */
    public Map<String, Object> getMemberRelations(Long memberId) {
        FamilyMember member = getMemberById(memberId);
        Map<String, Object> relations = new HashMap<>();

        // 父亲
        if (member.getParentId() != null) {
            FamilyMember father = memberMapper.selectById(member.getParentId());
            if (father != null && "M".equals(father.getGender())) {
                relations.put("father", father);
            }
        }

        // 配偶
        if (member.getSpouseId() != null) {
            FamilyMember spouse = memberMapper.selectById(member.getSpouseId());
            relations.put("spouse", spouse);
        }

        // 子女
        List<FamilyMember> children = memberMapper.selectChildren(memberId);
        relations.put("children", children);

        return relations;
    }

    /**
     * 搜索成员
     */
    public PageResult<FamilyMember> searchMembers(String keyword, Integer generation,
                                                   String branch, String gender,
                                                   int page, int pageSize) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getIsDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.like(FamilyMember::getName, keyword);
        }
        if (generation != null) {
            wrapper.eq(FamilyMember::getGeneration, generation);
        }
        if (StringUtils.hasText(branch)) {
            wrapper.eq(FamilyMember::getBranch, branch);
        }
        if (StringUtils.hasText(gender)) {
            wrapper.eq(FamilyMember::getGender, gender);
        }
        wrapper.orderByAsc(FamilyMember::getGeneration, FamilyMember::getSortOrder);

        Page<FamilyMember> pageObj = new Page<>(page, pageSize);
        memberMapper.selectPage(pageObj, wrapper);

        return PageResult.of(pageObj.getRecords(), pageObj.getTotal(), page, pageSize);
    }

    /**
     * 新增成员
     */
    @Transactional
    public FamilyMember createMember(FamilyMember member) {
        memberMapper.insert(member);

        // 维护闭包表
        if (member.getParentId() != null) {
            closureMapper.insertClosureForNewNode(member.getId(), member.getParentId());
        } else {
            closureMapper.insertClosureForRoot(member.getId());
        }

        log.info("新增族谱成员: id={}, name={}", member.getId(), member.getName());
        return member;
    }

    /**
     * 更新成员
     */
    @Transactional
    public FamilyMember updateMember(FamilyMember member) {
        FamilyMember existing = getMemberById(member.getId());

        // 如果父节点变更，需要重建闭包表
        if (member.getParentId() != null && !member.getParentId().equals(existing.getParentId())) {
            // 删除旧的闭包关系
            closureMapper.deleteClosureForSubtree(member.getId());
            // 重建新的闭包关系
            closureMapper.insertClosureForNewNode(member.getId(), member.getParentId());
            // 重建所有后代的闭包关系
            rebuildDescendantsClosure(member.getId());
        }

        memberMapper.updateById(member);
        log.info("更新族谱成员: id={}, name={}", member.getId(), member.getName());
        return member;
    }

    /**
     * 删除成员（逻辑删除）
     */
    @Transactional
    public void deleteMember(Long id) {
        FamilyMember member = getMemberById(id);
        // 检查是否有子节点
        List<FamilyMember> children = memberMapper.selectChildren(id);
        if (!children.isEmpty()) {
            throw new BusinessException("该成员有子节点，无法删除。请先删除或移动子节点。");
        }
        memberMapper.deleteById(id);
        log.info("删除族谱成员: id={}, name={}", id, member.getName());
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long total = memberMapper.selectCount(
                new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getIsDeleted, 0)
        );
        List<Integer> generations = memberMapper.selectAllGenerations();
        List<String> branches = memberMapper.selectAllBranches();

        stats.put("totalMembers", total);
        stats.put("generations", generations.size());
        stats.put("branches", branches.size());
        return stats;
    }

    /**
     * 获取所有世代
     */
    public List<Integer> getAllGenerations() {
        return memberMapper.selectAllGenerations();
    }

    /**
     * 获取所有房支
     */
    public List<String> getAllBranches() {
        return memberMapper.selectAllBranches();
    }

    /**
     * 重建后代的闭包关系（递归）
     */
    private void rebuildDescendantsClosure(Long parentId) {
        List<FamilyMember> children = memberMapper.selectChildren(parentId);
        for (FamilyMember child : children) {
            closureMapper.deleteClosureForSubtree(child.getId());
            closureMapper.insertClosureForNewNode(child.getId(), parentId);
            rebuildDescendantsClosure(child.getId());
        }
    }
}
