package com.expensetracker.repository;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.User;
import com.expensetracker.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByAdmin(Admin admin);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userGroup WHERE u.id = :userId")
    Optional<User> findByIdWithUserGroup(@Param("userId") Long userId);
    
    // Get all users managed by admin (directly or via groups)
    @Query("SELECT DISTINCT u FROM User u WHERE u.admin.id = :adminId OR u.userGroup.admin.id = :adminId")
    List<User> findByAdminManagedUsers(@Param("adminId") Long adminId);
    
    // Get users by group ID
    @Query("SELECT u FROM User u WHERE u.userGroup.id = :groupId")
    List<User> findByUserGroupId(@Param("groupId") Long groupId);
    
    // Get all admins and users without groups (Owner view - shows all admins, only users not in groups)
    @Query("SELECT u FROM User u WHERE u.role != :ownerRole AND (u.role = :adminRole OR u.userGroup IS NULL)")
    List<User> findAllExcludingOwnersAndGroupMembers(@Param("ownerRole") UserRole ownerRole, @Param("adminRole") UserRole adminRole);
}


