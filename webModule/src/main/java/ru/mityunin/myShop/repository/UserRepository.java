package ru.mityunin.myShop.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.mityunin.myShop.model.User;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByUsername(String username);
}