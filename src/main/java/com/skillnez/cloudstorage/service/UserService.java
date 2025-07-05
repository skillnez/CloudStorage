package com.skillnez.cloudstorage.service;

import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import com.skillnez.cloudstorage.entity.User;
import com.skillnez.cloudstorage.exception.UserAlreadyExistsException;
import com.skillnez.cloudstorage.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    protected void saveUser(UserRegistrationRequestDto userRegistrationRequestDto) throws ConstraintViolationException {
        User user = new User();
        user.setUsername(userRegistrationRequestDto.getUsername());
        user.setPassword(passwordEncoder.encode(userRegistrationRequestDto.getPassword()));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRoles(List.of("ROLE_USER"));
        userRepository.save(user);
    }

    public void registerUser(UserRegistrationRequestDto userRegistrationRequestDto) throws UserAlreadyExistsException {
        try {
            saveUser(userRegistrationRequestDto);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Account with this username "
                                                 + userRegistrationRequestDto.getUsername() + " already exists");
        }
    }
}
