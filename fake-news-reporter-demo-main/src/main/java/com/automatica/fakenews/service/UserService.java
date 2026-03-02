package com.automatica.fakenews.service;

import com.automatica.fakenews.dto.UserRegistrationDto;
import com.automatica.fakenews.model.User;
import com.automatica.fakenews.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void registerNewUser(UserRegistrationDto registrationDto) throws Exception {
        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            throw new Exception("Username already exists: " + registrationDto.getUsername());
        }

        if (registrationDto.getPassword() != null && !registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
             throw new Exception("Passwords do not match");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole("USER"); // Default role for regular viewers
        user.setEnabled(true);

        userRepository.save(user);
    }
}
