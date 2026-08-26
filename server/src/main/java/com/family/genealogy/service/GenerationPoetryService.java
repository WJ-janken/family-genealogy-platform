package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.family.genealogy.entity.GenerationPoetry;
import com.family.genealogy.mapper.GenerationPoetryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字辈诗/字辈歌业务服务层
 */
@Service
public class GenerationPoetryService extends ServiceImpl<GenerationPoetryMapper, GenerationPoetry> {

    /**
     * 根据姓氏和分支查询字辈诗
     *
     * @param surname   姓氏
     * @param branch    分支名称
     * @param page      页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    public Page<GenerationPoetry> getPageBySurnameAndBranch(String surname, String branch, int page, int pageSize) {
        QueryWrapper<GenerationPoetry> wrapper = new QueryWrapper<>();
        if (surname != null && !surname.isEmpty()) {
            wrapper.eq("surname", surname);
        }
        if (branch != null && !branch.isEmpty()) {
            wrapper.eq("branch_name", branch);
        }
        wrapper.orderByDesc("create_time");
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 根据姓氏获取所有字辈诗
     *
     * @param surname 姓氏
     * @return 字辈诗列表
     */
    public List<GenerationPoetry> getBySurname(String surname) {
        QueryWrapper<GenerationPoetry> wrapper = new QueryWrapper<>();
        wrapper.eq("surname", surname);
        wrapper.orderByAsc("start_generation");
        return this.list(wrapper);
    }

    /**
     * 获取启用的字辈诗列表
     *
     * @return 启用的字辈诗列表
     */
    public List<GenerationPoetry> getEnabledList() {
        QueryWrapper<GenerationPoetry> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", true);
        wrapper.orderByAsc("surname", "branch_name");
        return this.list(wrapper);
    }

    /**
     * 创建字辈诗
     *
     * @param generationPoetry 字辈诗对象
     * @return 保存后的字辈诗对象
     */
    public GenerationPoetry createGenerationPoetry(GenerationPoetry generationPoetry) {
        generationPoetry.setCreateTime(LocalDateTime.now());
        generationPoetry.setUpdateTime(LocalDateTime.now());
        generationPoetry.setEnabled(true);
        this.save(generationPoetry);
        return generationPoetry;
    }

    /**
     * 更新字辈诗
     *
     * @param generationPoetry 字辈诗对象
     * @return 更新后的字辈诗对象
     */
    public GenerationPoetry updateGenerationPoetry(GenerationPoetry generationPoetry) {
        GenerationPoetry existing = this.getById(generationPoetry.getId());
        if (existing != null) {
            existing.setTitle(generationPoetry.getTitle());
            existing.setSurname(generationPoetry.getSurname());
            existing.setBranchName(generationPoetry.getBranchName());
            existing.setGenerationSequence(generationPoetry.getGenerationSequence());
            existing.setInterpretation(generationPoetry.getInterpretation());
            existing.setStartGeneration(generationPoetry.getStartGeneration());
            existing.setRemark(generationPoetry.getRemark());
            existing.setUpdateTime(LocalDateTime.now());
            this.updateById(existing);
            return existing;
        }
        return null;
    }

    /**
     * 根据ID启用或禁用字辈诗
     *
     * @param id 字辈诗ID
     * @param enabled 启用状态
     * @return 操作结果
     */
    public boolean enableById(Long id, boolean enabled) {
        GenerationPoetry generationPoetry = this.getById(id);
        if (generationPoetry != null) {
            generationPoetry.setEnabled(enabled);
            generationPoetry.setUpdateTime(LocalDateTime.now());
            return this.updateById(generationPoetry);
        }
        return false;
    }
}