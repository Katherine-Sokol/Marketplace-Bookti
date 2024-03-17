package com.teamchallenge.bookti.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * UserEntity JPA Repository.
 *
 * @author Katherine Sokol
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findById(UUID id);

  boolean existsUserByEmail(String email);

  @Query(
      """
        select new com.teamchallenge.bookti.user.UserFullInfo(
          u.id, u.email, u.fullName, u.telegramId, u.creationDate, u.location, u.avatarUrl)
        from UserEntity u
        where u.id = :id
      """)
  Optional<UserFullInfo> findUserFullInfoById(@Param("id") UUID id);
}
