package com.assistencia.controller;

import com.assistencia.model.Conta;
import com.assistencia.model.Usuario;
import com.assistencia.repository.ContaRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.service.ContaTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contas")
public class ContaController {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ContaTaskService contaTaskService;

    @GetMapping
    public String listarContas(Model model) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return "redirect:/login";
        Long empresaId = logado.getEmpresa().getId();

        // 🚀 VERIFICAÇÃO AUTOMÁTICA: Recorrentes por empresa
        contaTaskService.processarContasRecorrentes();

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate fimMes = hoje.with(TemporalAdjusters.lastDayOfMonth());

        // 🔐 FILTRO SaaS: Busca apenas as contas da empresa logada
        List<Conta> todasAsContas = contaRepository.findByEmpresaIdOrderByDataVencimentoAsc(empresaId);

        // 1. FILTRO MENSAL (Tabela superior)
        List<Conta> contasDoMes = todasAsContas.stream()
                .filter(c -> !c.getDataVencimento().isBefore(inicioMes) &&
                        !c.getDataVencimento().isAfter(fimMes))
                .collect(Collectors.toList());

        // 2. CÁLCULOS DOS CARDS
        Double totalContas = contasDoMes.stream().mapToDouble(Conta::getValor).sum();
        Double totalPago = contasDoMes.stream().filter(Conta::isPaga).mapToDouble(Conta::getValor).sum();

        Double totalVencido = contasDoMes.stream()
                .filter(c -> !c.isPaga() && c.getDataVencimento().isBefore(hoje))
                .mapToDouble(Conta::getValor).sum();

        Double totalPendente = totalContas - totalPago;

        // 3. HISTÓRICO GERAL (Apenas pagas da empresa)
        List<Conta> historicoGeral = todasAsContas.stream()
                .filter(Conta::isPaga)
                .collect(Collectors.toList());

        model.addAttribute("dataInicio", inicioMes);
        model.addAttribute("dataFim", fimMes);
        model.addAttribute("totalContas", totalContas);
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("totalPendente", totalPendente);
        model.addAttribute("totalVencido", totalVencido);
        model.addAttribute("contas", contasDoMes);
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("novaConta", new Conta());

        return "contas";
    }

    @PostMapping("/pagar/{id}")
    public String marcarComoPago(@PathVariable Long id, Authentication auth, RedirectAttributes attributes) {
        Usuario logado = getUsuarioLogado();
        Conta conta = contaRepository.findById(id).orElse(null);

        // 🔐 SEGURANÇA SaaS: Verifica se a conta pertence à empresa do usuário
        if (conta != null && conta.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            conta.setPaga(true);
            conta.setUsuarioPagador(auth.getName());
            conta.setDataPagamento(LocalDateTime.now());
            contaRepository.save(conta);
            attributes.addFlashAttribute("mensagem", "Pagamento liquidado no sistema! 🦈");
        }
        return "redirect:/contas";
    }

    @PostMapping("/salvar")
    public String salvarConta(@ModelAttribute("novaConta") Conta conta,
                              @RequestParam("diaVencimento") int dia,
                              RedirectAttributes attributes) {
        try {
            Usuario logado = getUsuarioLogado();
            if (logado == null || logado.getEmpresa() == null) {
                attributes.addFlashAttribute("erro", "Sessão inválida ou empresa não identificada.");
                return "redirect:/login";
            }

            LocalDate hoje = LocalDate.now();
            int ultimoDiaDoMes = hoje.lengthOfMonth();

            if (dia < 1 || dia > ultimoDiaDoMes) {
                attributes.addFlashAttribute("erro", "O mês atual possui apenas " + ultimoDiaDoMes + " dias.");
                return "redirect:/contas";
            }

            conta.setDataVencimento(hoje.withDayOfMonth(dia));
            conta.setPaga(false);

            // 🔥 VINCULO SaaS: Carimba a conta com a empresa logada
            conta.setEmpresa(logado.getEmpresa());

            contaRepository.save(conta);
            attributes.addFlashAttribute("mensagem", "Conta lançada para o dia " + dia + " com sucesso! 🦈🚀");

        } catch (Exception e) {
            e.printStackTrace();
            attributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/contas";
    }

    @PostMapping("/deletar/{id}")
    public String deletarConta(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            Usuario logado = getUsuarioLogado();
            Conta conta = contaRepository.findById(id).orElse(null);

            // 🔐 SEGURANÇA SaaS: Verifica posse antes de deletar
            if (conta != null && conta.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                contaRepository.delete(conta);
                attributes.addFlashAttribute("mensagem", "Registro removido!");
            }
        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao remover registro.");
        }
        return "redirect:/contas";
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepo.findByUsername(login).orElse(null);
    }
}