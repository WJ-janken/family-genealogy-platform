package com.family.genealogy.controller;

import com.family.genealogy.common.PageResult;
import com.family.genealogy.common.Result;
import com.family.genealogy.entity.LocalChronicle;
import com.family.genealogy.service.LocalChronicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chronicles")
@RequiredArgsConstructor
public class LocalChronicleController {

    private final LocalChronicleService chronicleService;

    @GetMapping
    public Result<List<LocalChronicle>> getAll() {
        return Result.success(chronicleService.getAll());
    }

    @GetMapping("/page")
    public Result<PageResult<LocalChronicle>> getPage(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chronicleService.getList(category, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<LocalChronicle> getById(@PathVariable Long id) {
        return Result.success(chronicleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<LocalChronicle> create(@RequestBody LocalChronicle chronicle) {
        return Result.success(chronicleService.create(chronicle));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<LocalChronicle> update(@PathVariable Long id, @RequestBody LocalChronicle chronicle) {
        chronicle.setId(id);
        return Result.success(chronicleService.update(chronicle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        chronicleService.delete(id);
        return Result.success();
    }
}
