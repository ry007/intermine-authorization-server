package org.intermine.security.authserver.repository;

import org.intermine.security.authserver.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDetailRepository extends JpaRepository<Users,Integer> {
    Optional<Users> findByUsername(String name);
}