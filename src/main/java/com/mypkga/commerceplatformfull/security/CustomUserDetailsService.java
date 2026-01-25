package com.mypkga.commerceplatformfull.security;

import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find user by email first (since we're using email as login)
        User user = userRepository.findByEmail(username)
                .orElse(null);
        
        // If not found by email, try by username for backward compatibility
        if (user == null) {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        }

        log.debug("Loading user: {} with role: {}", username, user.getRoleName());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Use email as the principal
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                getAuthorities(user));
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add role-based authority
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
            
            // Add permission-based authorities
            List<String> permissions = user.getRole().getPermissionsList();
            for (String permission : permissions) {
                if (!"ALL".equals(permission)) {
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission));
                }
            }
            
            // If user has ALL permissions, add all possible permissions
            if (user.getRole().hasPermission("ALL")) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_ALL"));
            }
        }
        
        log.debug("User {} has authorities: {}", user.getUsername(), authorities);
        return authorities;
    }
}
