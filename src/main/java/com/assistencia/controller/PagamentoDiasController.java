package com.assistencia.controller;

import com.assistencia.model.Empresa;
import com.assistencia.model.Usuario;
import com.assistencia.repository.EmpresaRepository;
import com.assistencia.repository.UsuarioRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/pagamentos/assinatura")
public class PagamentoDiasController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @Value("${mercado_pago_sample_access_token}")
    private String mpAccessToken;

    public PagamentoDiasController(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/status-check")
    @ResponseBody
    public ResponseEntity<Boolean> verificarStatus(Authentication auth, @RequestParam("diasAnteriores") Integer diasAnteriores) {
        // 1. Proteção básica: Se não houver autenticação, retorna Proibido (403)
        if (auth == null) return ResponseEntity.status(403).build();

        // 2. Busca o usuário logado para chegar até a empresa vinculada
        // ✅ CORREÇÃO: Usamos .orElse(null) para abrir a caixa do Optional retornada pelo Repository
        Usuario u = usuarioRepository.findByUsername(auth.getName()).orElse(null);

        // Se o usuário não existir ou não tiver empresa vinculada, retorna falso
        if (u == null || u.getEmpresa() == null) {
            return ResponseEntity.ok(false);
        }

        // 3. Busca a versão mais recente da Empresa direto no banco de dados (MySQL)
        // ✅ CORREÇÃO: O findById sempre retorna Optional, o .orElse(null) resolve o Incompatible Types
        Empresa empresa = empresaRepository.findById(u.getEmpresa().getId()).orElse(null);

        // 4. Lógica de Verificação Shark Eletrônicos:
        // Se a empresa existir no banco e os 'diasRestantes' forem MAIORES que os dias que
        // ela tinha antes, significa que o Webhook do Mercado Pago já processou o pagamento com sucesso.
        boolean pago = (empresa != null && empresa.getDiasRestantes() > diasAnteriores);

        return ResponseEntity.ok(pago);
    }
}