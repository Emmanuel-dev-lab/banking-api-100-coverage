package com.bank.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountJpa, String> {
    List<AccountJpa> findByClientId(String clientId, Pageable pageable);

    long countByClientId(String clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpa a where a.id = :id")
    Optional<AccountJpa> findByIdForUpdate(@Param("id") String id);
}
