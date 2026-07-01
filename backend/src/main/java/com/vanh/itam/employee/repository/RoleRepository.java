// RoleRepository.java — JpaRepository<Role, Long>
package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

}