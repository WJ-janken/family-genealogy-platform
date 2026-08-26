package com.family.genealogy.controller;

import com.family.genealogy.common.PageResult;
import com.family.genealogy.common.Result;
import com.family.genealogy.entity.AuditRecord;
import com.family.genealogy.service.AuditService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审核控制器
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * 提交审核（普通用户）
     */
    @PostMapping("/submit")
    public Result<AuditRecord> submit(@RequestBody SubmitRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        AuditRecord record = auditService.submit(
                userId, request.getAction(), request.getTargetType(),
                request.getTargetId(), request.getData()
        );
        return Result.success(record);
    }

    /**
     * 审核通过（管理员）
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ReviewRequest request, Authentication auth) {
        Long reviewerId = (Long) auth.getPrincipal();
        auditService.approve(id, reviewerId, request.getComment());
        return Result.success();
    }

    /**
     * 审核驳回（管理员）
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody ReviewRequest request, Authentication auth) {
        Long reviewerId = (Long) auth.getPrincipal();
        auditService.reject(id, reviewerId, request.getComment());
        return Result.success();
    }

    /**
     * 获取待审核列表（管理员）
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<PageResult<AuditRecord>> getPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(auditService.getPendingList(page, pageSize));
    }

    /**
     * 获取审核列表（管理员）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<PageResult<AuditRecord>> getList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(auditService.getAuditList(status, page, pageSize));
    }

    /**
     * 获取我的提交记录
     */
    @GetMapping("/my-submissions")
    public Result<List<AuditRecord>> mySubmissions(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(auditService.getUserSubmissions(userId));
    }

    @Data
    static class SubmitRequest {
        private String action;
        private String targetType = "MEMBER";
        private Long targetId;
        private Map<String, Object> data;
    }

    @Data
    static class ReviewRequest {
        private String comment;
    }
}
