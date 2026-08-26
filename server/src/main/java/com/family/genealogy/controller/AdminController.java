package com.family.genealogy.controller;

import com.family.genealogy.common.PageResult;
import com.family.genealogy.common.Result;
import com.family.genealogy.entity.*;
import com.family.genealogy.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理统一控制器
 * 为 Vue3 后台管理系统提供接口
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminController {

    private final FamilyMemberService memberService;
    private final AuditService auditService;
    private final LocalChronicleService chronicleService;
    private final CareerRecordService careerService;
    private final GenerationPoetryService generationPoetryService;

    // ==================== 统计 ====================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(memberService.getStatistics());
    }

    @GetMapping("/audits/recent")
    public Result<List<AuditRecord>> getRecentAudits() {
        return Result.success(auditService.getRecentAudits(10));
    }

    @GetMapping("/logs/recent")
    public Result<List<Map<String, Object>>> getRecentLogs() {
        // 返回最近操作日志
        return Result.success(List.of());
    }

    // ==================== 成员管理 ====================

    @GetMapping("/members")
    public Result<PageResult<FamilyMember>> listMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(memberService.searchMembers(keyword, null, null, null, page, pageSize));
    }

    @GetMapping("/members/{id}")
    public Result<FamilyMember> getMember(@PathVariable Long id) {
        return Result.success(memberService.getMemberById(id));
    }

    @GetMapping("/members/tree")
    public Result<List<FamilyMember>> getMemberTree() {
        List<FamilyMember> flatNodes = memberService.getTreeNodes(null, 50);
        return Result.success(flatNodes);
    }

    @PostMapping("/members")
    public Result<FamilyMember> createMember(@RequestBody FamilyMember member) {
        return Result.success(memberService.createMember(member));
    }

    @PutMapping("/members/{id}")
    public Result<FamilyMember> updateMember(@PathVariable Long id, @RequestBody FamilyMember member) {
        member.setId(id);
        return Result.success(memberService.updateMember(member));
    }

    @DeleteMapping("/members/{id}")
    public Result<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return Result.success();
    }

    // ==================== 审核管理 ====================

    @GetMapping("/audits")
    public Result<PageResult<AuditRecord>> listAudits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(auditService.getAuditList(status, page, pageSize));
    }

    @PutMapping("/audits/{id}/approve")
    public Result<Void> approveAudit(@PathVariable Long id) {
        auditService.approve(id, null, null);
        return Result.success();
    }

    @PutMapping("/audits/{id}/reject")
    public Result<Void> rejectAudit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        auditService.reject(id, null, body.get("comment"));
        return Result.success();
    }

    // ==================== 地方志管理 ====================

    @GetMapping("/chronicles")
    public Result<PageResult<LocalChronicle>> listChronicles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chronicleService.getList(null, page, pageSize));
    }

    @PostMapping("/chronicles")
    public Result<LocalChronicle> createChronicle(@RequestBody LocalChronicle chronicle) {
        return Result.success(chronicleService.create(chronicle));
    }

    @PutMapping("/chronicles/{id}")
    public Result<LocalChronicle> updateChronicle(@PathVariable Long id, @RequestBody LocalChronicle chronicle) {
        chronicle.setId(id);
        return Result.success(chronicleService.update(chronicle));
    }

    @DeleteMapping("/chronicles/{id}")
    public Result<Void> deleteChronicle(@PathVariable Long id) {
        chronicleService.delete(id);
        return Result.success();
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        // 简化实现，返回空分页
        Map<String, Object> result = new HashMap<>();
        result.put("records", List.of());
        result.put("total", 0);
        return Result.success(result);
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // TODO: 实现角色更新
        return Result.success();
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // TODO: 实现状态更新
        return Result.success();
    }

    // ==================== 系统配置 ====================

    @GetMapping("/system/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("familyName", "");
        config.put("hallName", "");
        config.put("ancestorName", "");
        config.put("generationChars", "");
        config.put("generationStart", 1);
        config.put("defaultLayout", "vertical");
        config.put("pageSize", 20);
        return Result.success(config);
    }

    @PutMapping("/system/config")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> config) {
        // TODO: 保存到 sys_config 表
        return Result.success();
    }
    
    // ==================== 字辈诗管理 ====================
    
    /**
     * 根据姓氏获取字辈诗
     */
    @GetMapping("/generation-poetry/by-surname/{surname}")
    public Result<List<GenerationPoetry>> getGenerationPoetryBySurname(@PathVariable String surname) {
        return Result.success(generationPoetryService.getBySurname(surname));
    }

    // ==================== 操作日志 ====================

    @GetMapping("/logs")
    public Result<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", List.of());
        result.put("total", 0);
        return Result.success(result);
    }
}
