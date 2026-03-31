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
                                     @RequestParam("tipoComissao") String tipoComissao, // NOVO: Captura se é OS ou VENDA
                                     Authentication auth, // Captura o ADMIN logado
                                     RedirectAttributes attributes) {

        Usuario u = usuarioRepository.findById(id).orElse(null);

        if (u != null) {
            // 1. REGISTRAR O PAGAMENTO COM O TIPO ESPECÍFICO
            PagamentoComissao novoPagamento = new PagamentoComissao();
            novoPagamento.setFuncionarioId(u.getId());
            novoPagamento.setNomeFuncionario(u.getNome());
            novoPagamento.setValorPago(valor);
            novoPagamento.setTipoComissao(tipoComissao); // Define o que está sendo abatido
            novoPagamento.setDataHora(LocalDateTime.now());

            // Define quem está pagando (pega o nome do usuário logado no Spring Security)
            String adminNome = (auth != null) ? auth.getName() : "ADMIN";
            novoPagamento.setResponsavelPagamento(adminNome);

            pagamentoComissaoRepository.save(novoPagamento);

            attributes.addFlashAttribute("mensagem",
                    "Pagamento de " + tipoComissao + " para " + u.getNome() + " registrado!");
        }

        return "redirect:/funcionarios";
    }
}