package com.skillnez.cloudstorage;

import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import com.skillnez.cloudstorage.entity.User;
import com.skillnez.cloudstorage.exception.UserAlreadyExistsException;
import com.skillnez.cloudstorage.repository.UserRepository;
import com.skillnez.cloudstorage.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class UserServiceIntegrationTest {

    private final UserService userService;

    private final UserRepository userRepository;

    @Autowired
    UserServiceIntegrationTest(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Test
    public void shouldRegisterUser() {
        UserRegistrationRequestDto userRegistrationRequestDto = new UserRegistrationRequestDto(
                "skillnez", "password");
        userService.registerUser(userRegistrationRequestDto);
        User user = userRepository.findByUsername(userRegistrationRequestDto.getUsername());
        Assertions.assertEquals(userRegistrationRequestDto.getUsername(), user.getUsername());
    }

    @Test
    public void shouldNotRegisterDuplicateUser() {
        UserRegistrationRequestDto userRegistrationRequestDto = new UserRegistrationRequestDto(
                "skillnez", "password");
        userService.registerUser(userRegistrationRequestDto);
        Assertions.assertThrows(UserAlreadyExistsException.class, () -> {userService.registerUser(userRegistrationRequestDto);});
    }

    @Test
    public void passwordShouldBeEncoded() {
        UserRegistrationRequestDto userRegistrationRequestDto = new UserRegistrationRequestDto(
                "skillnez", "password");
        userService.registerUser(userRegistrationRequestDto);
        User user = userRepository.findByUsername(userRegistrationRequestDto.getUsername());
        Assertions.assertNotEquals(userRegistrationRequestDto.getPassword(), user.getPassword());
    }

    @Test
    public void userRoleShouldBeSpecified() {
        UserRegistrationRequestDto userRegistrationRequestDto = new UserRegistrationRequestDto(
                "skillnez", "password");
        userService.registerUser(userRegistrationRequestDto);
        User user = userRepository.findByUsername(userRegistrationRequestDto.getUsername());
        Assertions.assertEquals("ROLE_USER", user.getRoles().getFirst());
    }
}
