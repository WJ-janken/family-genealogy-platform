package com.family.genealogy.controller;

import com.family.genealogy.common.PageResult;
import com.family.genealogy.common.Result;
import com.family.genealogy.entity.FamilyMember;
import com.family.genealogy.service.FamilyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 族谱成员控制器
 */
@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService memberService;

    /**
     * 获取族谱树节点（扁平化列表）
     */
    @GetMapping("/tree")
    public Result<List<FamilyMember>> getTree(
            @RequestParam(required = false) Long rootId,
            @RequestParam(defaultValue = "10") int maxDepth) {
        return Result.success(memberService.getTreeNodes(rootId, maxDepth));
    }

    /**
     * 获取子节点（懒加载）
     */
    @GetMapping("/members/{parentId}/children")
    public Result<List<FamilyMember>> getChildren(@PathVariable Long parentId) {
        return Result.success(memberService.getChildren(parentId));
    }

    /**
     * 获取祖先链
     */
    @GetMapping("/members/{id}/ancestors")
    public Result<List<FamilyMember>> getAncestors(@PathVariable Long id) {
        return Result.success(memberService.getAncestors(id));
    }

    /**
     * 获取成员详情
     */
    @GetMapping("/members/{id}")
    public Result<FamilyMember> getMember(@PathVariable Long id) {
        return Result.success(memberService.getMemberById(id));
    }

    /**
     * 获取成员关系
     */
    @GetMapping("/members/{id}/relations")
    public Result<Map<String, Object>> getRelations(@PathVariable Long id) {
        return Result.success(memberService.getMemberRelations(id));
    }

    /**
     * 搜索成员
     */
    @GetMapping("/members/search")
    public Result<PageResult<FamilyMember>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String gender,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(memberService.searchMembers(keyword, generation, branch, gender, page, pageSize));
    }

    /**
     * 新增成员（管理员）
     */
    @PostMapping("/members")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<FamilyMember> create(@RequestBody FamilyMember member) {
        return Result.success(memberService.createMember(member));
    }

    /**
     * 更新成员（管理员）
     */
    @PutMapping("/members/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<FamilyMember> update(@PathVariable Long id, @RequestBody FamilyMember member) {
        member.setId(id);
        return Result.success(memberService.updateMember(member));
    }

    /**
     * 删除成员（管理员）
     */
    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.deleteMember(id);
        return Result.success();
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(memberService.getStatistics());
    }

    /**
     * 获取所有世代
     */
    @GetMapping("/generations")
    public Result<List<Integer>> getGenerations() {
        return Result.success(memberService.getAllGenerations());
    }

    /**
     * 获取所有房支
     */
    @GetMapping("/branches")
    public Result<List<String>> getBranches() {
        return Result.success(memberService.getAllBranches());
    }
}
