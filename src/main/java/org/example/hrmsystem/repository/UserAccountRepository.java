package org.example.hrmsystem.repository;

import java.util.Optional;
import org.example.hrmsystem.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
}
