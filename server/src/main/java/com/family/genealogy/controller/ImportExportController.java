package com.family.genealogy.controller;

import com.family.genealogy.common.Result;
import com.family.genealogy.service.ImportExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 导入导出控制器
 */
@RestController
@RequestMapping("/api/import-export")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;

    /**
     * 导出 Excel
     */
    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void exportExcel(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("族谱成员数据.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        importExportService.exportToExcel(response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导出 CSV
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void exportCsv(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("族谱成员数据.csv", StandardCharsets.UTF_8);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        // 写入 BOM 以确保 Excel 正确识别 UTF-8
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        String csvContent = importExportService.exportToCsv();
        response.getOutputStream().write(csvContent.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    /**
     * 导出 GEDCOM
     */
    @GetMapping("/export/gedcom")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void exportGedcom(HttpServletResponse response,
                             @RequestParam(required = false, defaultValue = "族谱") String familyName) throws IOException {
        String filename = URLEncoder.encode("族谱数据.ged", StandardCharsets.UTF_8);
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        String gedcomContent = importExportService.exportToGedcom(familyName);
        response.getOutputStream().write(gedcomContent.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    /**
     * 导入 Excel
     *
     * @param file 上传的 Excel 文件
     * @param mode 导入模式：overwrite-覆盖, append-追加, merge-合并
     */
    @PostMapping("/import/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "append") String mode) {
        Map<String, Object> result = importExportService.importFromExcel(file, mode);
        return Result.success(result);
    }

    /**
     * 导入 CSV
     */
    @PostMapping("/import/csv")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "append") String mode) {
        Map<String, Object> result = importExportService.importFromCsv(file, mode);
        return Result.success(result);
    }

    /**
     * 导入 GEDCOM
     */
    @PostMapping("/import/gedcom")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Map<String, Object>> importGedcom(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "append") String mode) {
        Map<String, Object> result = importExportService.importFromGedcom(file, mode);
        return Result.success(result);
    }

    /**
     * 获取导入模板（空 Excel 文件）
     */
    @GetMapping("/template/excel")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("族谱导入模板.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        // 导出空列表生成模板（只有表头）
        importExportService.exportToExcel(response.getOutputStream());
        response.flushBuffer();
    }
}
