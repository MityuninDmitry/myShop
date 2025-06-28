package ru.mityunin.myShop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.User;
import ru.mityunin.myShop.repository.UserRepository;

@Component
public class AdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private ReactiveOAuth2AuthorizedClientManager manager;


    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,ReactiveOAuth2AuthorizedClientManager manager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.manager = manager;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Before client registration {}");


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
        .then(userRepository.findByUsername("user1")
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setUsername("user1");
                    user.setPassword(passwordEncoder.encode("user1")); // Шифруем пароль
                    user.setRole("USER");
                    return userRepository.save(user);
                })))
        .then(userRepository.findByUsername("user2")
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setUsername("user2");
                    user.setPassword(passwordEncoder.encode("user2")); // Шифруем пароль
                    user.setRole("USER");
                    return userRepository.save(user);
                })))
        .subscribe();
    }


}
