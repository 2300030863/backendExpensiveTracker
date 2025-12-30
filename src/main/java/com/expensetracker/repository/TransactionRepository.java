package com.expensetracker.repository;

import com.expensetracker.entity.Admin;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    void deleteByUserId(Long userId);
    
    List<Transaction> findByUserId(Long userId);

    Page<Transaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    
    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);
    
    List<Transaction> findByUser(User user);
    
    List<Transaction> findByAdmin(Admin admin);

    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByUserAndCategoryIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByCategoryIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByUserAndAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Transaction> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndTypeAndDateRange(@Param("user") User user,
                                                  @Param("type") TransactionType type,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndTypeAndCategoryAndDateRange(@Param("user") User user,
                                                             @Param("type") TransactionType type,
                                                             @Param("categoryId") Long categoryId,
                                                             @Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT t.category.name, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> getCategoryWiseSpending(@Param("user") User user,
                                           @Param("type") TransactionType type,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT DATE_FORMAT(t.transactionDate, '%Y-%m'), COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(t.transactionDate, '%Y-%m') " +
           "ORDER BY DATE_FORMAT(t.transactionDate, '%Y-%m')")
    List<Object[]> getMonthlyTrend(@Param("user") User user,
                                   @Param("type") TransactionType type,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // For user groups - sum amount by group and type (excluding admin)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.userGroup.id = :groupId AND t.user.role = 'ROLE_USER' AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserGroupAndTypeAndDateRange(@Param("groupId") Long groupId,
                                                       @Param("type") TransactionType type,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    // For user groups - category wise spending (excluding admin)
    @Query("SELECT t.category.name, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.userGroup.id = :groupId AND t.user.role = 'ROLE_USER' AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> getCategoryWiseSpendingByUserGroup(@Param("groupId") Long groupId,
                                                      @Param("type") TransactionType type,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    // For user groups - monthly trend (excluding admin)
    @Query("SELECT DATE_FORMAT(t.transactionDate, '%Y-%m'), COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.userGroup.id = :groupId AND t.user.role = 'ROLE_USER' AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(t.transactionDate, '%Y-%m') " +
           "ORDER BY DATE_FORMAT(t.transactionDate, '%Y-%m')")
    List<Object[]> getMonthlyTrendByUserGroup(@Param("groupId") Long groupId,
                                              @Param("type") TransactionType type,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // For user groups - sum amount by group, type and category (excluding admin)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.userGroup.id = :groupId AND t.user.role = 'ROLE_USER' AND t.type = :type AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserGroupAndTypeAndCategoryAndDateRange(@Param("groupId") Long groupId,
                                                                  @Param("type") TransactionType type,
                                                                  @Param("categoryId") Long categoryId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    // For users in admin groups - get transactions from all users in the same group (excluding admin)
    @Query("SELECT t FROM Transaction t WHERE t.user.userGroup.id = :groupId AND t.user.role = 'ROLE_USER' ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserGroupOrderByTransactionDateDesc(@Param("groupId") Long groupId, Pageable pageable);
    
    // For users in admin groups - get transactions from group members only (NOT including admin)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.user.userGroup.id = :groupId " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserGroupIncludingAdminOrderByTransactionDateDesc(@Param("groupId") Long groupId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user.userGroup.id = :groupId " +
           "AND t.user.role = 'ROLE_USER' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserGroupAndDateRange(@Param("groupId") Long groupId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    // For users in admin groups - get transactions from group members only (NOT including admin) with date range
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.user.userGroup.id = :groupId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserGroupIncludingAdminAndDateRange(@Param("groupId") Long groupId,
                                                                 @Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.userGroup.id = :groupId " +
           "AND t.user.role = 'ROLE_USER' " +
           "AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserGroupAndCategoryAndDateRange(@Param("groupId") Long groupId,
                                                              @Param("categoryId") Long categoryId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.userGroup.id = :groupId " +
           "AND t.user.role = 'ROLE_USER' " +
           "AND t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserGroupAndAccountAndDateRange(@Param("groupId") Long groupId,
                                                             @Param("accountId") Long accountId,
                                                             @Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);
    
    // For admins - get transactions from admin and all users they manage
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN t.user u " +
           "LEFT JOIN u.userGroup ug " +
           "WHERE t.user = :adminUser OR u.admin = :admin OR ug.admin = :admin " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAdminManagedUsersOrderByTransactionDateDesc(@Param("adminUser") User adminUser, 
                                                                         @Param("admin") Admin admin, 
                                                                         Pageable pageable);
    
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN t.user u " +
           "LEFT JOIN u.userGroup ug " +
           "WHERE (t.user = :adminUser OR u.admin = :admin OR ug.admin = :admin) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAdminManagedUsersAndDateRange(@Param("adminUser") User adminUser,
                                                          @Param("admin") Admin admin,
                                                           @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN t.user u " +
           "LEFT JOIN u.userGroup ug " +
           "WHERE (t.user = :adminUser OR u.admin = :admin OR ug.admin = :admin) " +
           "AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAdminManagedUsersAndCategoryAndDateRange(@Param("adminUser") User adminUser,
                                                                     @Param("admin") Admin admin,
                                                                     @Param("categoryId") Long categoryId,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT DISTINCT t FROM Transaction t " +
           "LEFT JOIN t.user u " +
           "LEFT JOIN u.userGroup ug " +
           "WHERE (t.user = :adminUser OR u.admin = :admin OR ug.admin = :admin) " +
           "AND t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAdminManagedUsersAndAccountAndDateRange(@Param("adminUser") User adminUser,
                                                                    @Param("admin") Admin admin,
                                                                    @Param("accountId") Long accountId,
                                                                    @Param("startDate") LocalDate startDate,
                                                                    @Param("endDate") LocalDate endDate);
    
    // Monthly trend for admin's managed users
    @Query("SELECT DATE_FORMAT(t.transactionDate, '%Y-%m'), COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "LEFT JOIN t.user u " +
           "LEFT JOIN u.userGroup ug " +
           "WHERE (t.user = :adminUser OR u.admin = :admin OR ug.admin = :admin) " +
           "AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(t.transactionDate, '%Y-%m') " +
           "ORDER BY DATE_FORMAT(t.transactionDate, '%Y-%m')")
    List<Object[]> getMonthlyTrendByAdminManagedUsers(@Param("adminUser") User adminUser,
                                                      @Param("admin") Admin admin,
                                                      @Param("type") TransactionType type,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
}


