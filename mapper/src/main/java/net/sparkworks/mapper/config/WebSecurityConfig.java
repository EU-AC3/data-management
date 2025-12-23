package net.sparkworks.mapper.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    @Order(-1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .requestMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeRequests(authorize -> authorize
                        .antMatchers("/actuator/prometheus").permitAll()
                        .antMatchers("/**").authenticated()
                )
                .csrf().disable()
                .httpBasic().disable();
        return http.build();
    }
}