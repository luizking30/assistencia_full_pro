package com.assistencia.controller;

import com.assistencia.model.Conta;
import com.assistencia.repository.ContaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contas")
public class ContaController {

    @Autowired
    private ContaRepository contaRepository;

    @GetMapping
    public String listarContas(Model model) {
        LocalDate hoje = LocalDate.now();
        // DATA DE COMEÇO E FIM DO MÊS ATUAL
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        List<Conta> todasAsContas = contaRepository.findAllByOrderByDataVencimentoAsc();

        // 1. FILTRO: Apenas contas que vencem/pertencem ao mês atual
        List<Conta> contasDoMes = todasAsContas.stream()
                .filter(c -> !c.getDataVencimento().isBefore(inicioMes) &&
                        !c.getDataVencimento().isAfter(fimMes))
                .collect(Collectors.toList());

        // 2. CÁLCULOS PARA OS 3 CARDS (Baseado apenas no mês atual)
        Double totalContas = contasDoMes.stream()
                .mapToDouble(Conta::getValor).sum();

        Double totalPendente = contasDoMes.stream()
                .filter(c -> !c.isPago())
                .mapToDouble(Conta::getValor).sum();

        Double totalPago = contasDoMes.stream()
                .filter(c -> c.isPago())
                .mapToDouble(Conta::getValor).sum();

        // 3. LISTAS PARA AS TABELAS
        // Tabela de cima: Tudo que ainda não foi pago (de qualquer mês)
        List<Conta> pendentes = todasAsContas.stream()
                .filter(c -> !c.isPago())
                .collect(Collectors.toList());

        // Tabela de baixo: Tudo que já foi pago (Histórico)
        List<Conta> historico = todasAsContas.stream()
                .filter(c -> c.isPago())
                .collect(Collectors.toList());

        // Atributos para os Cards
        model.addAttribute("dataInicio", inicioMes);
        model.addAttribute("dataFim", fimMes);
        model.addAttribute("totalContas", totalContas);
        model.addAttribute("totalPendente", totalPendente);
        model.addAttribute("totalPago", totalPago);

        // Atributos para as Tabelas e Forms
        model.addAttribute("contas", pendentes);
        model.addAttribute("historico", historico);
        model.addAttribute("novaConta", new Conta());

        return "contas";
    }

    @PostMapping("/pagar/{id}")
    public String marcarComoPago(@PathVariable Long id, RedirectAttributes attributes) {
        Conta contaOriginal = contaRepository.findById(id).orElse(null);

        if (contaOriginal != null) {
            if (contaOriginal.isRecorrente()) {
                // 1. Cria o registro estático (cópia) para o histórico
                Conta registroHistorico = new Conta();

                String mesNome = contaOriginal.getDataVencimento()
                        .getMonth()
                        .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));

                registroHistorico.setDescricao(contaOriginal.getDescricao() + " - Ref: " + mesNome);
                registroHistorico.setValor(contaOriginal.getValor());
                registroHistorico.setDataVencimento(contaOriginal.getDataVencimento());
                registroHistorico.setRecorrente(false);
                registroHistorico.setPago(true);
                contaRepository.save(registroHistorico);

                // 2. Renova a conta original para o mês seguinte
                contaOriginal.setDataVencimento(contaOriginal.getDataVencimento().plusMonths(1));
                contaOriginal.setPago(false);
                contaRepository.save(contaOriginal);

                attributes.addFlashAttribute("mensagem", "Pagamento de " + mesNome + " registrado! Conta renovada.");
            } else {
                contaOriginal.setPago(true);
                contaRepository.save(contaOriginal);
                attributes.addFlashAttribute("mensagem", "Conta liquidada!");
            }
        }
        return "redirect:/contas";
    }

    @PostMapping("/salvar")
    public String salvarConta(@ModelAttribute("novaConta") Conta conta, RedirectAttributes attributes) {
        try {
            if (conta.getDataVencimento().isBefore(LocalDate.now())) {
                attributes.addFlashAttribute("erro", "Não é permitido lançar datas passadas!");
                return "redirect:/contas";
            }
            conta.setPago(false);
            contaRepository.save(conta);
            attributes.addFlashAttribute("mensagem", "Nova conta lançada com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/contas";
    }

    @PostMapping("/deletar/{id}")
    public String deletarConta(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            contaRepository.deleteById(id);
            attributes.addFlashAttribute("mensagem", "Registro removido com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao remover registro.");
        }
        return "redirect:/contas";
    }
}