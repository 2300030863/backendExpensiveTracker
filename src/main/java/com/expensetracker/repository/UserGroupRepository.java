package com.expensetracker.repository;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByAdmin(Admin admin);
}
