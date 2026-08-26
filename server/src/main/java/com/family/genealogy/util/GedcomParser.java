package com.family.genealogy.util;

import com.family.genealogy.entity.FamilyMember;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * GEDCOM 格式解析器
 * 支持 GEDCOM 5.5 标准的基本解析和生成
 */
@Slf4j
public class GedcomParser {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy")
    };

    /**
     * 解析 GEDCOM 文件，返回族谱成员列表
     */
    public static List<FamilyMember> parseGedcom(InputStream inputStream) throws IOException {
        List<GedcomRecord> records = parseRecords(inputStream);
        return convertToMembers(records);
    }

    /**
     * 将族谱成员列表导出为 GEDCOM 格式字符串
     */
    public static String exportToGedcom(List<FamilyMember> members, String familyName) {
        StringBuilder sb = new StringBuilder();

        // GEDCOM Header
        sb.append("0 HEAD\n");
        sb.append("1 SOUR FamilyGenealogy\n");
        sb.append("2 VERS 1.0\n");
        sb.append("2 NAME 家族族谱管理系统\n");
        sb.append("1 DEST ANY\n");
        sb.append("1 DATE ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)).toUpperCase()).append("\n");
        sb.append("1 GEDC\n");
        sb.append("2 VERS 5.5.1\n");
        sb.append("2 FORM LINEAGE-LINKED\n");
        sb.append("1 CHAR UTF-8\n");
        sb.append("1 NOTE ").append(familyName != null ? familyName : "族谱").append("数据导出\n");

        // Individual records
        Map<Long, String> idToXref = new HashMap<>();
        int xrefCounter = 1;

        for (FamilyMember member : members) {
            String xref = "@I" + xrefCounter + "@";
            idToXref.put(member.getId(), xref);
            xrefCounter++;
        }

        // Family records tracking
        Map<String, String> familyXrefs = new HashMap<>();
        int famCounter = 1;

        for (FamilyMember member : members) {
            String xref = idToXref.get(member.getId());
            sb.append("0 ").append(xref).append(" INDI\n");
            sb.append("1 NAME ").append(member.getName() != null ? member.getName() : "").append("\n");

            // Gender
            if (member.getGender() != null) {
                sb.append("1 SEX ").append(member.getGender()).append("\n");
            }

            // Birth
            if (member.getBirthDate() != null || member.getBirthPlace() != null) {
                sb.append("1 BIRT\n");
                if (member.getBirthDate() != null) {
                    sb.append("2 DATE ").append(formatGedcomDate(member.getBirthDate())).append("\n");
                }
                if (member.getBirthPlace() != null && !member.getBirthPlace().isEmpty()) {
                    sb.append("2 PLAC ").append(member.getBirthPlace()).append("\n");
                }
            }

            // Death
            if (member.getDeathDate() != null) {
                sb.append("1 DEAT\n");
                sb.append("2 DATE ").append(formatGedcomDate(member.getDeathDate())).append("\n");
            }

            // Note (biography)
            if (member.getBiography() != null && !member.getBiography().isEmpty()) {
                sb.append("1 NOTE ").append(member.getBiography().replace("\n", " ")).append("\n");
            }

            // Custom tags for extended data
            if (member.getGeneration() != null) {
                sb.append("1 _GEN ").append(member.getGeneration()).append("\n");
            }
            if (member.getBranch() != null && !member.getBranch().isEmpty()) {
                sb.append("1 _BRAN ").append(member.getBranch()).append("\n");
            }

            // Family links (FAMC - child of family)
            if (member.getParentId() != null) {
                String famKey = member.getParentId() + "-" + (member.getSpouseId() != null ? member.getSpouseId() : "0");
                String famXref = familyXrefs.computeIfAbsent(famKey, k -> "@F" + (familyXrefs.size() + 1) + "@");
                sb.append("1 FAMC ").append(famXref).append("\n");
            }
        }

        // Family records
        for (Map.Entry<String, String> entry : familyXrefs.entrySet()) {
            String[] parts = entry.getKey().split("-");
            Long husbId = Long.parseLong(parts[0]);
            Long wifeId = "0".equals(parts[1]) ? null : Long.parseLong(parts[1]);

            sb.append("0 ").append(entry.getValue()).append(" FAM\n");
            if (idToXref.containsKey(husbId)) {
                sb.append("1 HUSB ").append(idToXref.get(husbId)).append("\n");
            }
            if (wifeId != null && idToXref.containsKey(wifeId)) {
                sb.append("1 WIFE ").append(idToXref.get(wifeId)).append("\n");
            }

            // Add children
            for (FamilyMember member : members) {
                if (husbId.equals(member.getParentId())) {
                    String childXref = idToXref.get(member.getId());
                    if (childXref != null) {
                        sb.append("1 CHIL ").append(childXref).append("\n");
                    }
                }
            }
        }

        // Trailer
        sb.append("0 TRLR\n");

        return sb.toString();
    }

    // ==================== 内部解析逻辑 ====================

    private static List<GedcomRecord> parseRecords(InputStream inputStream) throws IOException {
        List<GedcomRecord> records = new ArrayList<>();
        GedcomRecord currentRecord = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int level = parseLevel(line);
                if (level < 0) continue;

                String rest = line.substring(String.valueOf(level).length()).trim();

                if (level == 0) {
                    // 新的顶级记录
                    if (rest.contains("INDI")) {
                        currentRecord = new GedcomRecord();
                        currentRecord.type = "INDI";
                        currentRecord.xref = extractXref(rest);
                        records.add(currentRecord);
                    } else if (rest.contains("FAM")) {
                        currentRecord = new GedcomRecord();
                        currentRecord.type = "FAM";
                        currentRecord.xref = extractXref(rest);
                        records.add(currentRecord);
                    } else {
                        currentRecord = null;
                    }
                } else if (currentRecord != null) {
                    // 子级标签
                    String tag = extractTag(rest);
                    String value = extractValue(rest, tag);
                    currentRecord.addProperty(level, tag, value);
                }
            }
        }

        return records;
    }

    private static List<FamilyMember> convertToMembers(List<GedcomRecord> records) {
        List<FamilyMember> members = new ArrayList<>();
        Map<String, FamilyMember> xrefToMember = new HashMap<>();
        Map<String, List<String>> familyChildren = new HashMap<>(); // FAM xref -> children xrefs
        Map<String, String> familyHusband = new HashMap<>(); // FAM xref -> husband xref
        Map<String, String> childToFamily = new HashMap<>(); // INDI xref -> FAM xref (FAMC)

        long idCounter = 1;

        // 第一遍：解析个人记录
        for (GedcomRecord record : records) {
            if ("INDI".equals(record.type)) {
                FamilyMember member = new FamilyMember();
                member.setId(idCounter++);
                member.setSortOrder(0);

                String name = record.getProperty("NAME");
                if (name != null) {
                    member.setName(name.replace("/", "").trim());
                }

                String sex = record.getProperty("SEX");
                member.setGender("F".equals(sex) ? "F" : "M");

                String birthDate = record.getNestedProperty("BIRT", "DATE");
                if (birthDate != null) {
                    member.setBirthDate(parseGedcomDate(birthDate));
                }

                String birthPlace = record.getNestedProperty("BIRT", "PLAC");
                if (birthPlace != null) {
                    member.setBirthPlace(birthPlace);
                }

                String deathDate = record.getNestedProperty("DEAT", "DATE");
                if (deathDate != null) {
                    member.setDeathDate(parseGedcomDate(deathDate));
                }

                String note = record.getProperty("NOTE");
                if (note != null) {
                    member.setBiography(note);
                }

                String gen = record.getProperty("_GEN");
                if (gen != null) {
                    try {
                        member.setGeneration(Integer.parseInt(gen));
                    } catch (NumberFormatException ignored) {
                    }
                }

                String branch = record.getProperty("_BRAN");
                if (branch != null) {
                    member.setBranch(branch);
                }

                String famc = record.getProperty("FAMC");
                if (famc != null) {
                    childToFamily.put(record.xref, famc);
                }

                xrefToMember.put(record.xref, member);
                members.add(member);
            } else if ("FAM".equals(record.type)) {
                String husb = record.getProperty("HUSB");
                if (husb != null) {
                    familyHusband.put(record.xref, husb);
                }

                List<String> children = record.getProperties("CHIL");
                if (!children.isEmpty()) {
                    familyChildren.put(record.xref, children);
                }
            }
        }

        // 第二遍：建立父子关系
        for (Map.Entry<String, String> entry : childToFamily.entrySet()) {
            String childXref = entry.getKey();
            String famXref = entry.getValue();

            FamilyMember child = xrefToMember.get(childXref);
            String husbXref = familyHusband.get(famXref);

            if (child != null && husbXref != null) {
                FamilyMember father = xrefToMember.get(husbXref);
                if (father != null) {
                    child.setParentId(father.getId());
                }
            }
        }

        // 自动推算世代（如果没有 _GEN 标签）
        assignGenerations(members);

        return members;
    }

    private static void assignGenerations(List<FamilyMember> members) {
        // 检查是否已有世代信息
        boolean hasGeneration = members.stream().anyMatch(m -> m.getGeneration() != null && m.getGeneration() > 0);
        if (hasGeneration) return;

        // 构建 ID -> Member 映射
        Map<Long, FamilyMember> idMap = new HashMap<>();
        for (FamilyMember m : members) {
            idMap.put(m.getId(), m);
        }

        // 找到根节点（无父节点）
        for (FamilyMember m : members) {
            if (m.getParentId() == null) {
                m.setGeneration(1);
                assignChildGeneration(m, idMap, members);
            }
        }
    }

    private static void assignChildGeneration(FamilyMember parent, Map<Long, FamilyMember> idMap, List<FamilyMember> members) {
        for (FamilyMember m : members) {
            if (parent.getId().equals(m.getParentId())) {
                m.setGeneration(parent.getGeneration() + 1);
                assignChildGeneration(m, idMap, members);
            }
        }
    }

    private static int parseLevel(String line) {
        try {
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) return -1;
            return Integer.parseInt(line.substring(0, spaceIdx));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String extractXref(String rest) {
        if (rest.startsWith("@")) {
            int end = rest.indexOf('@', 1);
            if (end > 0) {
                return rest.substring(0, end + 1);
            }
        }
        return "";
    }

    private static String extractTag(String rest) {
        // 可能是 "@xref@ TAG" 或 "TAG value"
        String[] parts = rest.split("\\s+", 2);
        if (parts[0].startsWith("@")) {
            // 跳过 xref
            if (parts.length > 1) {
                String[] subParts = parts[1].split("\\s+", 2);
                return subParts[0];
            }
            return "";
        }
        return parts[0];
    }

    private static String extractValue(String rest, String tag) {
        int tagIdx = rest.indexOf(tag);
        if (tagIdx < 0) return "";
        int valueStart = tagIdx + tag.length();
        if (valueStart >= rest.length()) return "";
        return rest.substring(valueStart).trim();
    }

    private static LocalDate parseGedcomDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;

        // 移除 GEDCOM 日期前缀（ABT, BEF, AFT, etc.）
        dateStr = dateStr.replaceAll("^(ABT|BEF|AFT|EST|CAL|FROM|TO|BET|AND)\\s+", "").trim();

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr.toUpperCase(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        // 尝试只有年份的情况
        try {
            int year = Integer.parseInt(dateStr.trim());
            if (year > 0 && year < 3000) {
                return LocalDate.of(year, 1, 1);
            }
        } catch (NumberFormatException ignored) {
        }

        log.debug("无法解析 GEDCOM 日期: {}", dateStr);
        return null;
    }

    private static String formatGedcomDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)).toUpperCase();
    }

    // ==================== 内部数据结构 ====================

    private static class GedcomRecord {
        String type;
        String xref;
        List<GedcomProperty> properties = new ArrayList<>();

        void addProperty(int level, String tag, String value) {
            properties.add(new GedcomProperty(level, tag, value));
        }

        String getProperty(String tag) {
            for (GedcomProperty prop : properties) {
                if (tag.equals(prop.tag)) {
                    return prop.value;
                }
            }
            return null;
        }

        List<String> getProperties(String tag) {
            List<String> values = new ArrayList<>();
            for (GedcomProperty prop : properties) {
                if (tag.equals(prop.tag)) {
                    values.add(prop.value);
                }
            }
            return values;
        }

        String getNestedProperty(String parentTag, String childTag) {
            boolean inParent = false;
            int parentLevel = -1;
            for (GedcomProperty prop : properties) {
                if (parentTag.equals(prop.tag)) {
                    inParent = true;
                    parentLevel = prop.level;
                } else if (inParent) {
                    if (prop.level <= parentLevel) {
                        inParent = false;
                    } else if (childTag.equals(prop.tag)) {
                        return prop.value;
                    }
                }
            }
            return null;
        }
    }

    private static class GedcomProperty {
        int level;
        String tag;
        String value;

        GedcomProperty(int level, String tag, String value) {
            this.level = level;
            this.tag = tag;
            this.value = value;
        }
    }
}
