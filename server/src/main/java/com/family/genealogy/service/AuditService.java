package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.genealogy.common.BusinessException;
import com.family.genealogy.common.Constants;
import com.family.genealogy.common.PageResult;
import com.family.genealogy.entity.AuditRecord;
import com.family.genealogy.entity.FamilyMember;
import com.family.genealogy.mapper.AuditRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审核服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRecordMapper auditMapper;
    private final FamilyMemberService memberService;
    private final ObjectMapper objectMapper;

    /**
     * 提交审核
     */
    public AuditRecord submit(Long submitterId, String action, String targetType,
                              Long targetId, Object data) {
        AuditRecord record = new AuditRecord();
        record.setSubmitterId(submitterId);
        record.setAction(action);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setStatus(Constants.AUDIT_PENDING);

        try {
            record.setAfterData(objectMapper.writeValueAsString(data));
            // 如果是修改操作，保存修改前数据
            if (Constants.ACTION_UPDATE.equals(action) && targetId != null) {
                if ("MEMBER".equals(targetType)) {
                    FamilyMember existing = memberService.getMemberById(targetId);
                    record.setBeforeData(objectMapper.writeValueAsString(existing));
                }
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据序列化失败");
        }

        auditMapper.insert(record);
        log.info("提交审核: id={}, action={}, targetType={}", record.getId(), action, targetType);
        return record;
    }

    /**
     * 审核通过
     */
    @Transactional
    public void approve(Long auditId, Long reviewerId, String comment) {
        AuditRecord record = getAuditRecord(auditId);
        if (!Constants.AUDIT_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该记录不在待审核状态");
        }

        record.setStatus(Constants.AUDIT_APPROVED);
        record.setReviewerId(reviewerId);
        record.setReviewComment(comment);
        record.setReviewedAt(LocalDateTime.now());
        auditMapper.updateById(record);

        // 执行数据合并
        mergeData(record);
        log.info("审核通过: id={}", auditId);
    }

    /**
     * 审核驳回
     */
    public void reject(Long auditId, Long reviewerId, String comment) {
        AuditRecord record = getAuditRecord(auditId);
        if (!Constants.AUDIT_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该记录不在待审核状态");
        }

        record.setStatus(Constants.AUDIT_REJECTED);
        record.setReviewerId(reviewerId);
        record.setReviewComment(comment);
        record.setReviewedAt(LocalDateTime.now());
        auditMapper.updateById(record);
        log.info("审核驳回: id={}", auditId);
    }

    /**
     * 获取待审核列表
     */
    public PageResult<AuditRecord> getPendingList(int page, int pageSize) {
        Page<AuditRecord> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<AuditRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditRecord::getStatus, Constants.AUDIT_PENDING)
               .orderByDesc(AuditRecord::getCreatedAt);
        auditMapper.selectPage(pageObj, wrapper);
        return PageResult.of(pageObj.getRecords(), pageObj.getTotal(), page, pageSize);
    }

    /**
     * 获取审核记录列表（全部状态）
     */
    public PageResult<AuditRecord> getAuditList(String status, int page, int pageSize) {
        Page<AuditRecord> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<AuditRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AuditRecord::getStatus, status);
        }
        wrapper.orderByDesc(AuditRecord::getCreatedAt);
        auditMapper.selectPage(pageObj, wrapper);
        return PageResult.of(pageObj.getRecords(), pageObj.getTotal(), page, pageSize);
    }

    /**
     * 获取用户的提交记录
     */
    public List<AuditRecord> getUserSubmissions(Long userId) {
        return auditMapper.selectList(
                new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getSubmitterId, userId)
                        .orderByDesc(AuditRecord::getCreatedAt)
        );
    }

    /**
     * 获取最近审核记录
     */
    public List<AuditRecord> getRecentAudits(int limit) {
        return auditMapper.selectList(
                new LambdaQueryWrapper<AuditRecord>()
                        .orderByDesc(AuditRecord::getCreatedAt)
                        .last("LIMIT " + limit)
        );
    }

    private AuditRecord getAuditRecord(Long id) {
        AuditRecord record = auditMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        return record;
    }

    /**
     * 数据合并到正式表
     */
    private void mergeData(AuditRecord record) {
        try {
            if ("MEMBER".equals(record.getTargetType())) {
                FamilyMember member = objectMapper.readValue(record.getAfterData(), FamilyMember.class);
                switch (record.getAction()) {
                    case Constants.ACTION_CREATE -> memberService.createMember(member);
                    case Constants.ACTION_UPDATE -> {
                        member.setId(record.getTargetId());
                        memberService.updateMember(member);
                    }
                    case Constants.ACTION_DELETE -> memberService.deleteMember(record.getTargetId());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("数据合并失败: auditId={}", record.getId(), e);
            throw new BusinessException("数据合并失败");
        }
    }
}
