package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.genealogy.entity.CareerRecord;
import com.family.genealogy.mapper.CareerRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerRecordService {

    private final CareerRecordMapper careerMapper;

    public List<CareerRecord> getByMemberId(Long memberId) {
        return careerMapper.selectList(
                new LambdaQueryWrapper<CareerRecord>()
                        .eq(CareerRecord::getMemberId, memberId)
                        .eq(CareerRecord::getIsDeleted, 0)
                        .orderByAsc(CareerRecord::getSortOrder)
        );
    }

    public CareerRecord create(CareerRecord record) {
        careerMapper.insert(record);
        return record;
    }

    public CareerRecord update(CareerRecord record) {
        careerMapper.updateById(record);
        return record;
    }

    public void delete(Long id) {
        careerMapper.deleteById(id);
    }
}
