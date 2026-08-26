package com.family.genealogy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.genealogy.entity.GenerationPoetry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字辈诗/字辈歌数据访问层
 */
@Mapper
public interface GenerationPoetryMapper extends BaseMapper<GenerationPoetry> {
}