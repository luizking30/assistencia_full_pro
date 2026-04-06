package com.assistencia.controller;

import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/pagamentos/comissoes")
public class PagamentoComissaoController {

    private final UsuarioRepository usuarioRepository;
    private final PagamentoComissaoRepository pagamentoComissaoRepository;

    public PagamentoComissaoController(UsuarioRepository usuarioRepository,
                                       PagamentoComissaoRepository pagamentoComissaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pagamentoComissaoRepository = pagamentoComissaoRepository;
    }

    @PostMapping("/registrar")
    @Transactional // Garante que o registro do pagamento e o zeramento do saldo ocorram juntos
    public String registrarPagamento(@RequestParam("funcionarioId") Long id,
                                     @RequestParam("valorPago") Double valor,
                                     @RequestParam("tipoComissao") String tipoComissao,
                                     Authentication auth,
                                     RedirectAttributes attributes) {

        // ✅ CORREÇÃO APLICADA: .orElse(null) resolve o erro de Incompatible Types (Optional -> Usuario)
        Usuario u = usuarioRepository.findById(id).orElse(null);

        if (u != null && valor != null && valor > 0.01) {
            PagamentoComissao novoPagamento = new PagamentoComissao();
            novoPagamento.setFuncionarioId(u.getId());
            novoPagamento.setNomeFuncionario(u.getNome());
            novoPagamento.setValorPago(valor);
            novoPagamento.setTipoComissao(tipoComissao.toUpperCase());
            novoPagamento.setDataHora(LocalDateTime.now());

            String adminNome = (auth != null) ? auth.getName() : "ADMIN_SHARK";
            novoPagamento.setResponsavelPagamento(adminNome);

            // 1. Salva o histórico do pagamento
            pagamentoComissaoRepository.save(novoPagamento);

            // 2. Zera o saldo do funcionário no banco de dados conforme o tipo selecionado
            if (tipoComissao.equalsIgnoreCase("TOTAL")) {
                u.setComissaoOs(0.0);
                u.setComissaoVenda(0.0);
            } else if (tipoComissao.equalsIgnoreCase("OS")) {
                u.setComissaoOs(0.0);
            } else if (tipoComissao.equalsIgnoreCase("VENDA")) {
                u.setComissaoVenda(0.0);
            }

            // 3. Persiste a alteração do saldo no MySQL
            usuarioRepository.save(u);

            attributes.addFlashAttribute("mensagem", "Comissão de " + u.getNome() + " liquidada com sucesso!");
        } else {
            attributes.addFlashAttribute("erro", "Dados inválidos: Usuário não encontrado ou valor insuficiente.");
        }

        return "redirect:/admin/funcionarios";
    }
}