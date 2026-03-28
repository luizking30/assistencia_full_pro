package com.assistencia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Liberando login, registro e recursos estáticos
                        .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/images/**").permitAll()

                        // Restringindo a nova área de gestão apenas para ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Qualquer outra rota exige estar logado
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // O sucessHandler agora pode ser simplificado ou mantido para o dashboard
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Reativar CSRF é recomendado para produção, mas mantendo conforme seu código anterior
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* NOTA: O 'InMemoryUserDetailsManager' foi removido.
       Para o sistema funcionar com o seu banco de dados, você deve criar uma classe
       'UserDetailsService' que busque o 'Usuario' no banco e verifique o campo 'aprovado'.
    */
}