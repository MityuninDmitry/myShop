package ru.mityunin.myShop.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.User;
import ru.mityunin.myShop.repository.UserRepository;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository.deleteAll().then(
                        userRepository.findByUsername("admin")
                                .switchIfEmpty(Mono.defer(() -> {
                                    User admin = new User();
                                    admin.setUsername("admin");
                                    admin.setPassword(passwordEncoder.encode("admin")); // Шифруем пароль
                                    admin.setRole("ADMIN");
                                    return userRepository.save(admin);
                                }))
        )
        .subscribe();
    }
}
