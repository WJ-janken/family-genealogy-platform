package com.family.genealogy.controller;

import com.family.genealogy.common.PageResult;
import com.family.genealogy.common.Result;
import com.family.genealogy.entity.GenerationPoetry;
import com.family.genealogy.service.GenerationPoetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 字辈诗/字辈歌管理控制器
 */
@RestController
@RequestMapping("/api/generation-poetry")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class GenerationPoetryController {

    private final GenerationPoetryService generationPoetryService;

    /**
     * 分页查询字辈诗
     */
    @GetMapping
    public Result<PageResult<GenerationPoetry>> listGenerationPoetry(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) String branch) {
        var pageInfo = generationPoetryService.getPageBySurnameAndBranch(surname, branch, page, pageSize);
        return Result.success(PageResult.from(pageInfo));
    }

    /**
     * 获取所有启用的字辈诗
     */
    @GetMapping("/enabled")
    public Result<java.util.List<GenerationPoetry>> getEnabledList() {
        return Result.success(generationPoetryService.getEnabledList());
    }

    /**
     * 根据姓氏获取字辈诗
     */
    @GetMapping("/by-surname/{surname}")
    public Result<java.util.List<GenerationPoetry>> getBySurname(@PathVariable String surname) {
        return Result.success(generationPoetryService.getBySurname(surname));
    }

    /**
     * 获取单个字辈诗详情
     */
    @GetMapping("/{id}")
    public Result<GenerationPoetry> getGenerationPoetry(@PathVariable Long id) {
        return Result.success(generationPoetryService.getById(id));
    }

    /**
     * 创建字辈诗
     */
    @PostMapping
    public Result<GenerationPoetry> createGenerationPoetry(@RequestBody GenerationPoetry generationPoetry) {
        return Result.success(generationPoetryService.createGenerationPoetry(generationPoetry));
    }

    /**
     * 更新字辈诗
     */
    @PutMapping("/{id}")
    public Result<GenerationPoetry> updateGenerationPoetry(
            @PathVariable Long id,
            @RequestBody GenerationPoetry generationPoetry) {
        generationPoetry.setId(id);
        return Result.success(generationPoetryService.updateGenerationPoetry(generationPoetry));
    }

    /**
     * 删除字辈诗
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteGenerationPoetry(@PathVariable Long id) {
        generationPoetryService.removeById(id);
        return Result.success();
    }

    /**
     * 启用/禁用字辈诗
     */
    @PutMapping("/{id}/enable")
    public Result<Void> enableGenerationPoetry(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        generationPoetryService.enableById(id, enabled);
        return Result.success();
    }
}