package com.assistencia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
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
                        // LIBERADO: Rotas da Shark Eletrônicos que não exigem login
                        .requestMatchers(
                                "/login",
                                "/registro",
                                "/registro-funcionario",
                                "/registro-empresa",
                                "/api/webhook/**",
                                "/pagamento/**",
                                "/esqueci-senha",
                                "/resetar-senha",      // Removido o /** para aceitar query params ?token=
                                "/atualizar-senha",    // Removido o /** para focar na rota POST
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error/**"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        // Handler customizado: identifica se o erro é conta desabilitada (não aprovada)
                        .failureHandler((request, response, exception) -> {
                            // Se o erro for de conta desabilitada, envia o parâmetro not_approved
                            if (exception instanceof DisabledException ||
                                    (exception.getCause() != null && exception.getCause() instanceof DisabledException)) {
                                response.sendRedirect("/login?error=true&not_approved=true");
                            } else {
                                // Erro comum de usuário ou senha
                                response.sendRedirect("/login?error=true");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Desabilitado para Webhooks e desenvolvimento (Cuidado em produção real de S.I.)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}