package com.onlinebanking.security;

import com.onlinebanking.entity.User;
import com.onlinebanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new BankUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toSet())
        );
    }
}
