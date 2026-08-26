package com.family.genealogy.controller;

import com.family.genealogy.common.Result;
import com.family.genealogy.entity.CareerRecord;
import com.family.genealogy.service.CareerRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareerRecordController {

    private final CareerRecordService careerService;

    @GetMapping("/member/{memberId}")
    public Result<List<CareerRecord>> getByMember(@PathVariable Long memberId) {
        return Result.success(careerService.getByMemberId(memberId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<CareerRecord> create(@RequestBody CareerRecord record) {
        return Result.success(careerService.create(record));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<CareerRecord> update(@PathVariable Long id, @RequestBody CareerRecord record) {
        record.setId(id);
        return Result.success(careerService.update(record));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        careerService.delete(id);
        return Result.success();
    }
}
