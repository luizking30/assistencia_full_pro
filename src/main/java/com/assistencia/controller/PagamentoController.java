package com.assistencia.controller;

import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final UsuarioRepository usuarioRepository;
    private final PagamentoComissaoRepository pagamentoComissaoRepository;

    public PagamentoController(UsuarioRepository usuarioRepository,
                               PagamentoComissaoRepository pagamentoComissaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pagamentoComissaoRepository = pagamentoComissaoRepository;
    }

    @PostMapping("/registrar")
    public String registrarPagamento(@RequestParam("funcionarioId") Long id,
                                     @RequestParam("valorPago") Double valor,
                                     @RequestParam("tipoComissao") String tipoComissao,
                                     Authentication auth,
                                     RedirectAttributes attributes) {

        // 1. Busca o usuário com segurança
        Usuario u = usuarioRepository.findById(id).orElse(null);

        if (u != null && valor != null && valor > 0) {
            // 2. Criar o registro do histórico de pagamentos
            PagamentoComissao novoPagamento = new PagamentoComissao();
            novoPagamento.setFuncionarioId(u.getId());
            novoPagamento.setNomeFuncionario(u.getNome());
            novoPagamento.setValorPago(valor);

            // Garante que o tipo salve como "OS" ou "VENDA" (conforme seu repository espera)
            novoPagamento.setTipoComissao(tipoComissao.toUpperCase());
            novoPagamento.setDataHora(LocalDateTime.now());

            // 3. Captura quem é o administrador que está realizando a baixa
            String adminNome = (auth != null) ? auth.getName() : "ADMIN_SHARK";
            novoPagamento.setResponsavelPagamento(adminNome);

            // 4. Salva no banco (Isso vai abater automaticamente no cálculo do AdminController)
            pagamentoComissaoRepository.save(novoPagamento);

            attributes.addFlashAttribute("mensagem",
                    "Pagamento de " + tipoComissao + " para " + u.getNome() + " registrado com sucesso!");
        } else {
            attributes.addFlashAttribute("erro", "Erro ao registrar: Verifique o valor informado.");
        }

        // ✅ CORREÇÃO: Redireciona para a rota correta que você está usando no AdminController
        return "redirect:/admin/funcionarios";
    }
}