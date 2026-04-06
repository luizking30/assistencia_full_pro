package com.assistencia.controller;

import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.model.Empresa;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Adicione este import
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
    @Transactional // Garante que a operação seja atômica
    public String registrarPagamento(@RequestParam("funcionarioId") Long id,
                                     @RequestParam("valorPago") Double valor,
                                     @RequestParam("tipoComissao") String tipoComissao,
                                     Authentication auth,
                                     RedirectAttributes attributes) {

        Usuario u = usuarioRepository.findById(id).orElse(null);

        // Se o valor for quase zero (ex: 0.000001), tratamos como 0 para não bugar
        if (u != null && valor != null && valor > 0.001) {

            PagamentoComissao novoPagamento = new PagamentoComissao();
            novoPagamento.setFuncionarioId(u.getId());
            novoPagamento.setNomeFuncionario(u.getNome());
            novoPagamento.setValorPago(valor);
            novoPagamento.setTipoComissao(tipoComissao.toUpperCase()); // "TOTAL" vindo do HTML
            novoPagamento.setDataHora(LocalDateTime.now());

            String adminNome = (auth != null) ? auth.getName() : "ADMIN_SHARK";
            novoPagamento.setResponsavelPagamento(adminNome);

            pagamentoComissaoRepository.save(novoPagamento);

            attributes.addFlashAttribute("mensagem", "Comissão de " + u.getNome() + " liquidada com sucesso!");
        } else {
            attributes.addFlashAttribute("erro", "Não há saldo para liquidar ou usuário inválido.");
        }

        return "redirect:/admin/funcionarios";
    }
}