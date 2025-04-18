package com.healthmanagement.dao.member;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.member.ResetToken;
import com.healthmanagement.model.member.User;

/**
 * 重設密碼令牌資料存取接口
 */
@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Integer> {

    /**
     * 通過令牌值查找重設密碼令牌
     * 
     * @param token 令牌值
     * @return 查找結果
     */
    Optional<ResetToken> findByToken(String token);

    /**
     * 通過用戶查找重設密碼令牌
     * 
     * @param user 用戶
     * @return 查找結果
     */
    Optional<ResetToken> findByUser(User user);

    /**
     * 通過用戶ID查找重設密碼令牌
     * 
     * @param userId 用戶ID
     * @return 查找結果
     */
    Optional<ResetToken> findByUserId(Integer userId);

    /**
     * 刪除用戶的所有重設密碼令牌
     * 
     * @param user 用戶
     */
    void deleteByUser(User user);
}