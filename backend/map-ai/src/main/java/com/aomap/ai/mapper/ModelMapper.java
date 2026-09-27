package com.aomap.ai.mapper;

import com.aomap.ai.domain.ModelRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelMapper {

    List<ModelRecord> list();

    ModelRecord findById(@Param("id") long id);

    ModelRecord findEnabled();

    int insert(ModelRecord model);

    int update(ModelRecord model);

    int delete(@Param("id") long id);

    int disableAll();

    int enable(@Param("id") long id);
}
