package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.genealogy.common.BusinessException;
import com.family.genealogy.common.PageResult;
import com.family.genealogy.entity.LocalChronicle;
import com.family.genealogy.mapper.LocalChronicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalChronicleService {

    private final LocalChronicleMapper chronicleMapper;

    public PageResult<LocalChronicle> getList(String category, int page, int pageSize) {
        Page<LocalChronicle> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<LocalChronicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocalChronicle::getIsDeleted, 0);
        if (StringUtils.hasText(category)) {
            wrapper.eq(LocalChronicle::getCategory, category);
        }
        wrapper.orderByAsc(LocalChronicle::getSortOrder);
        chronicleMapper.selectPage(pageObj, wrapper);
        return PageResult.of(pageObj.getRecords(), pageObj.getTotal(), page, pageSize);
    }

    public List<LocalChronicle> getAll() {
        return chronicleMapper.selectList(
                new LambdaQueryWrapper<LocalChronicle>()
                        .eq(LocalChronicle::getIsDeleted, 0)
                        .orderByAsc(LocalChronicle::getSortOrder)
        );
    }

    public LocalChronicle getById(Long id) {
        LocalChronicle chronicle = chronicleMapper.selectById(id);
        if (chronicle == null || chronicle.getIsDeleted() == 1) {
            throw new BusinessException("地方志不存在");
        }
        return chronicle;
    }

    public LocalChronicle create(LocalChronicle chronicle) {
        chronicleMapper.insert(chronicle);
        return chronicle;
    }

    public LocalChronicle update(LocalChronicle chronicle) {
        chronicleMapper.updateById(chronicle);
        return chronicle;
    }

    public void delete(Long id) {
        chronicleMapper.deleteById(id);
    }
}
