package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.genealogy.common.BusinessException;
import com.family.genealogy.entity.FamilyMember;
import com.family.genealogy.mapper.FamilyMemberMapper;
import com.family.genealogy.mapper.MemberClosureMapper;
import com.family.genealogy.util.ExcelUtil;
import com.family.genealogy.util.GedcomParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入导出服务
 * 支持 Excel(.xlsx)、CSV、GEDCOM 格式的族谱数据导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private final FamilyMemberMapper memberMapper;
    private final MemberClosureMapper closureMapper;
    private final FamilyMemberService familyMemberService;

    /**
     * 导出族谱成员到 Excel
     */
    public void exportToExcel(OutputStream outputStream) throws IOException {
        List<FamilyMember> members = getAllMembers();
        ExcelUtil.exportMembers(members, outputStream);
        log.info("导出 Excel 完成，共 {} 条记录", members.size());
    }

    /**
     * 导出族谱成员到 CSV
     */
    public String exportToCsv() {
        List<FamilyMember> members = getAllMembers();
        log.info("导出 CSV 完成，共 {} 条记录", members.size());
        return ExcelUtil.exportMembersToCsv(members);
    }

    /**
     * 导出族谱成员到 GEDCOM 格式
     */
    public String exportToGedcom(String familyName) {
        List<FamilyMember> members = getAllMembers();
        log.info("导出 GEDCOM 完成，共 {} 条记录", members.size());
        return GedcomParser.exportToGedcom(members, familyName);
    }

    /**
     * 从 Excel 文件导入族谱成员
     *
     * @param file 上传的 Excel 文件
     * @param mode 导入模式：overwrite-覆盖, append-追加, merge-合并（按ID匹配更新）
     * @return 导入结果统计
     */
    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file, String mode) {
        validateFile(file, ".xlsx", ".xls");

        try {
            List<FamilyMember> members = ExcelUtil.importMembers(file.getInputStream());
            return processImport(members, mode);
        } catch (IOException e) {
            log.error("Excel 导入失败", e);
            throw new BusinessException("Excel 文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 从 CSV 文件导入族谱成员
     */
    @Transactional
    public Map<String, Object> importFromCsv(MultipartFile file, String mode) {
        validateFile(file, ".csv");

        try {
            // CSV 使用与 Excel 相同的解析逻辑（POI 支持 CSV）
            List<FamilyMember> members = ExcelUtil.importMembers(file.getInputStream());
            return processImport(members, mode);
        } catch (IOException e) {
            log.error("CSV 导入失败", e);
            throw new BusinessException("CSV 文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 从 GEDCOM 文件导入族谱成员
     */
    @Transactional
    public Map<String, Object> importFromGedcom(MultipartFile file, String mode) {
        validateFile(file, ".ged", ".gedcom");

        try {
            List<FamilyMember> members = GedcomParser.parseGedcom(file.getInputStream());
            return processImport(members, mode);
        } catch (IOException e) {
            log.error("GEDCOM 导入失败", e);
            throw new BusinessException("GEDCOM 文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 处理导入逻辑
     */
    private Map<String, Object> processImport(List<FamilyMember> members, String mode) {
        if (members.isEmpty()) {
            throw new BusinessException("导入文件中没有有效数据");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        switch (mode != null ? mode : "append") {
            case "overwrite":
                // 清空现有数据后导入
                clearAllMembers();
                for (FamilyMember member : members) {
                    try {
                        insertMemberWithClosure(member);
                        created++;
                    } catch (Exception e) {
                        log.warn("导入成员失败: name={}, error={}", member.getName(), e.getMessage());
                        failed++;
                    }
                }
                break;

            case "merge":
                // 按 ID 匹配，存在则更新，不存在则新增
                for (FamilyMember member : members) {
                    try {
                        if (member.getId() != null) {
                            FamilyMember existing = memberMapper.selectById(member.getId());
                            if (existing != null && existing.getIsDeleted() == 0) {
                                memberMapper.updateById(member);
                                updated++;
                                continue;
                            }
                        }
                        insertMemberWithClosure(member);
                        created++;
                    } catch (Exception e) {
                        log.warn("导入成员失败: name={}, error={}", member.getName(), e.getMessage());
                        failed++;
                    }
                }
                break;

            case "append":
            default:
                // 追加模式：忽略 ID，全部作为新记录插入
                // 需要维护 parentId 映射关系
                Map<Long, Long> oldToNewIdMap = new HashMap<>();
                for (FamilyMember member : members) {
                    try {
                        Long oldId = member.getId();
                        Long oldParentId = member.getParentId();
                        member.setId(null);

                        // 如果父节点已经在本次导入中创建，使用新 ID
                        if (oldParentId != null && oldToNewIdMap.containsKey(oldParentId)) {
                            member.setParentId(oldToNewIdMap.get(oldParentId));
                        }

                        insertMemberWithClosure(member);
                        created++;

                        // 记录旧 ID 到新 ID 的映射
                        if (oldId != null) {
                            oldToNewIdMap.put(oldId, member.getId());
                        }
                    } catch (Exception e) {
                        log.warn("导入成员失败: name={}, error={}", member.getName(), e.getMessage());
                        failed++;
                    }
                }
                break;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", members.size());
        result.put("created", created);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("failed", failed);

        log.info("导入完成: total={}, created={}, updated={}, skipped={}, failed={}",
                members.size(), created, updated, skipped, failed);

        return result;
    }

    /**
     * 插入成员并维护闭包表
     */
    private void insertMemberWithClosure(FamilyMember member) {
        member.setIsDeleted(0);
        memberMapper.insert(member);

        // 维护闭包表
        if (member.getParentId() != null) {
            closureMapper.insertClosureForNewNode(member.getId(), member.getParentId());
        } else {
            closureMapper.insertClosureForRoot(member.getId());
        }
    }

    /**
     * 清空所有成员数据（覆盖模式使用）
     */
    private void clearAllMembers() {
        // 先清空闭包表
        closureMapper.deleteAll();
        // 物理删除所有成员（覆盖导入场景）
        memberMapper.delete(new LambdaQueryWrapper<>());
        log.warn("覆盖导入：已清空所有族谱成员数据");
    }

    /**
     * 获取所有未删除的成员
     */
    private List<FamilyMember> getAllMembers() {
        return memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getIsDeleted, 0)
                        .orderByAsc(FamilyMember::getGeneration, FamilyMember::getSortOrder)
        );
    }

    /**
     * 校验上传文件
     */
    private void validateFile(MultipartFile file, String... allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("无法获取文件名");
        }

        boolean valid = false;
        for (String ext : allowedExtensions) {
            if (originalFilename.toLowerCase().endsWith(ext)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            throw new BusinessException("不支持的文件格式，请上传 " + String.join("/", allowedExtensions) + " 格式文件");
        }

        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 10MB");
        }
    }
}
