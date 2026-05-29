package com.onlinebanking.config;

import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize roles if not already present
        if (roleRepository.count() == 0) {
            Role customerRole = new Role();
            customerRole.setName(RoleName.ROLE_CUSTOMER);
            
            Role adminRole = new Role();
            adminRole.setName(RoleName.ROLE_ADMIN);
            
            roleRepository.save(customerRole);
            roleRepository.save(adminRole);
            
            System.out.println("Initialized default roles: ROLE_CUSTOMER, ROLE_ADMIN");
        }

        // Create default admin user if not already present
        if (userRepository.findByEmail("admin@onlinebanking.com").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
            
            User adminUser = new User();
            adminUser.setEmail("admin@onlinebanking.com");
            adminUser.setPassword(passwordEncoder.encode("admin@123456"));
            adminUser.setFirstName("System");
            adminUser.setLastName("Administrator");
            adminUser.setPhone("+1234567890");
            adminUser.setEnabled(true);
            adminUser.setRoles(Set.of(adminRole));
            
            userRepository.save(adminUser);
            System.out.println("Created default admin user: admin@onlinebanking.com / admin@123456");
        }
    }
}
