package com.aomap.workspace.mapper;

import com.aomap.workspace.domain.ProjectRow;
import com.aomap.workspace.domain.ShareRow;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ProjectMapper {

    List<ProjectRow> list(@Param("orgId") long orgId);

    ProjectRow find(@Param("orgId") long orgId, @Param("id") long id);

    int insert(ProjectRow row);

    int update(@Param("orgId") long orgId, @Param("id") long id, @Param("name") String name, @Param("state") String state);

    int delete(@Param("orgId") long orgId, @Param("id") long id);

    int insertShare(@Param("projectId") long projectId, @Param("token") String token,
                    @Param("passwordHash") String passwordHash, @Param("expiresAt") OffsetDateTime expiresAt);

    ShareRow findShare(@Param("token") String token);
}
