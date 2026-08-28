package com.example.bookmark.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserDataSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "admin123", Role.ADMIN);
        seed("user", "user123", Role.USER);
    }

    private void seed(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
