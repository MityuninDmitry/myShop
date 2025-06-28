package ru.mityunin.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().hasRole("WEB_APP")
                )
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Получаем доступ к resource_access
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("webModule");
            List<String> clientRoles = (List<String>) clientAccess.get("roles");

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> realmRoles = (List<String>) realmAccess.get("roles");

            // Объединяем роли
            List<String> allRoles = new ArrayList<>();
            if (clientRoles != null) allRoles.addAll(clientRoles);
            if (realmRoles != null) allRoles.addAll(realmRoles);

            return allRoles.stream()
                    .map(role -> "ROLE_" + role) // Добавляем префикс
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }
}