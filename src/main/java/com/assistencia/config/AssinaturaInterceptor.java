package com.assistencia.config;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AssinaturaInterceptor implements HandlerInterceptor {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authName = SecurityContextHolder.getContext().getAuthentication().getName();
        String uri = request.getRequestURI();

        // 1. Deixa passar se for anônimo, recursos estáticos ou a própria página de pagamento/logout
        if (authName.equals("anonymousUser") ||
                uri.startsWith("/pagamento") ||
                uri.startsWith("/logout") ||
                uri.startsWith("/css") ||
                uri.startsWith("/js") ||
                uri.startsWith("/images")) {
            return true;
        }

        // 2. Busca o usuário e checa a empresa
        Usuario usuario = usuarioRepository.findByUsername(authName).orElse(null);

        if (usuario != null && usuario.getEmpresa() != null) {
            // Se os dias acabaram, redireciona para o pagamento
            if (usuario.getEmpresa().getDiasRestantes() <= 0) {
                response.sendRedirect("/pagamento");
                return false; // Cancela a requisição original (não carrega o Dashboard/Vendas)
            }
        }

        return true;
    }
}