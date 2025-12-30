package com.expensetracker.repository;

import com.expensetracker.entity.Account;
import com.expensetracker.entity.Admin;
import com.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    void deleteByUserId(Long userId);
    
    List<Account> findByUserAndIsActiveTrueOrderByName(User user);
    
    List<Account> findByUserOrderByName(User user);
    
    // For users in admin groups - get accounts from all users in the same group
    @Query("SELECT a FROM Account a WHERE a.user.userGroup.id = :groupId AND a.isActive = true ORDER BY a.name")
    List<Account> findByUserGroupAndIsActiveTrueOrderByName(@Param("groupId") Long groupId);
    
    // For users in admin groups - get accounts from group members and their admin
    @Query("SELECT a FROM Account a WHERE " +
           "(a.user.userGroup.id = :groupId OR " +
           "(a.user.role = 'ROLE_ADMIN' AND EXISTS (SELECT 1 FROM UserGroup ug WHERE ug.id = :groupId AND ug.admin.username = a.user.username))) " +
           "AND a.isActive = true ORDER BY a.name")
    List<Account> findByUserGroupIncludingAdminAndIsActiveTrueOrderByName(@Param("groupId") Long groupId);
    
    // For admins - get accounts from all users they manage (including admin's own)
    @Query("SELECT DISTINCT a FROM Account a WHERE " +
           "(a.user.username = :adminUsername OR a.user.admin.username = :adminUsername OR a.user.userGroup.admin.username = :adminUsername) " +
           "AND a.isActive = true ORDER BY a.name")
    List<Account> findByAdminManagedUsersAndIsActiveTrueOrderByName(@Param("adminUsername") String adminUsername);
}



