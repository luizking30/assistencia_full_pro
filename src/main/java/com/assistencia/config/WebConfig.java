package com.assistencia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AssinaturaInterceptor assinaturaInterceptor;

    /**
     * Resolve o erro 404 da raiz (/).
     * Sempre que alguém acessar http://localhost:8080/, será mandado para o /dashboard.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/dashboard");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(assinaturaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",                   // Permite a raiz (o redirecionamento acima cuida dela)
                        "/login",
                        "/registro-empresa",
                        "/pagamento/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"               // IMPORTANTE: Exclui a página de erro para não entrar em loop no 404
                );
    }
}