package com.onlinebanking.repository;

import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
