package com.assistencia.controller;

import com.assistencia.model.Conta;
import com.assistencia.repository.ContaRepository;
import com.assistencia.service.ContaTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
    private ContaTaskService contaTaskService;

    @GetMapping
    public String listarContas(Model model) {
        // 🚀 VERIFICAÇÃO AUTOMÁTICA:
        // Se o mês virou, o serviço cria as contas recorrentes no banco antes de listar.
        contaTaskService.processarContasRecorrentes();

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate fimMes = hoje.with(TemporalAdjusters.lastDayOfMonth());

        List<Conta> todasAsContas = contaRepository.findAllByOrderByDataVencimentoAsc();

        // 1. FILTRO MENSAL (Tabela superior)
        List<Conta> contasDoMes = todasAsContas.stream()
                .filter(c -> !c.getDataVencimento().isBefore(inicioMes) &&
                        !c.getDataVencimento().isAfter(fimMes))
                .collect(Collectors.toList());

        // 2. CÁLCULOS DOS CARDS
        Double totalContas = contasDoMes.stream().mapToDouble(Conta::getValor).sum();
        Double totalPago = contasDoMes.stream().filter(Conta::isPaga).mapToDouble(Conta::getValor).sum();

        // 🚀 NOVO CÁLCULO: Soma apenas o que NÃO está pago e já passou da data de hoje
        Double totalVencido = contasDoMes.stream()
                .filter(c -> !c.isPaga() && c.getDataVencimento().isBefore(hoje))
                .mapToDouble(Conta::getValor).sum();

        Double totalPendente = totalContas - totalPago;

        // 3. HISTÓRICO GERAL (Tabela inferior - Todo o período)
        List<Conta> historicoGeral = todasAsContas.stream()
                .filter(Conta::isPaga)
                .collect(Collectors.toList());

        // Atributos para o Thymeleaf
        model.addAttribute("dataInicio", inicioMes);
        model.addAttribute("dataFim", fimMes);
        model.addAttribute("totalContas", totalContas);
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("totalPendente", totalPendente);
        model.addAttribute("totalVencido", totalVencido); // 🦈 Novo Atributo
        model.addAttribute("contas", contasDoMes);
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("novaConta", new Conta());

        return "contas";
    }

    @PostMapping("/pagar/{id}")
    public String marcarComoPago(@PathVariable Long id, Authentication auth, RedirectAttributes attributes) {
        Conta conta = contaRepository.findById(id).orElse(null);
        if (conta != null) {
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
            LocalDate hoje = LocalDate.now();
            int ultimoDiaDoMes = hoje.lengthOfMonth();

            if (dia < 1 || dia > ultimoDiaDoMes) {
                attributes.addFlashAttribute("erro", "O mês atual possui apenas " + ultimoDiaDoMes + " dias.");
                return "redirect:/contas";
            }

            // Sistema monta a data: Dia escolhido + Mês e Ano atuais
            conta.setDataVencimento(hoje.withDayOfMonth(dia));
            conta.setPaga(false);
            contaRepository.save(conta);
            attributes.addFlashAttribute("mensagem", "Conta lançada para o dia " + dia + " com sucesso! 🦈🚀");

        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/contas";
    }

    @PostMapping("/deletar/{id}")
    public String deletarConta(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            contaRepository.deleteById(id);
            attributes.addFlashAttribute("mensagem", "Registro removido!");
        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao remover registro.");
        }
        return "redirect:/contas";
    }
}