package com.aomap.auth.mapper;

import com.aomap.auth.domain.MembershipRow;
import com.aomap.auth.domain.OrgRow;
import com.aomap.auth.domain.UserRow;
import org.apache.ibatis.annotations.Param;

public interface AuthMapper {

    UserRow findByEmail(String email);

    int insertUser(UserRow user);

    int insertOrg(OrgRow org);

    int insertMember(@Param("orgId") long orgId, @Param("userId") long userId, @Param("role") String role);

    MembershipRow findMembership(long userId);
}
