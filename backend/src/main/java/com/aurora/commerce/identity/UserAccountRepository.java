package com.aurora.commerce.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
