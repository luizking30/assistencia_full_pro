package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FuncionarioController {

    private final UsuarioRepository usuarioRepo;
    private final OrdemServicoRepository ordemServicoRepo;
    private final VendaRepository vendaRepo;
    private final PagamentoComissaoRepository pagamentoRepo;

    public FuncionarioController(UsuarioRepository usuarioRepo,
                                 OrdemServicoRepository ordemServicoRepo,
                                 VendaRepository vendaRepo,
                                 PagamentoComissaoRepository pagamentoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.ordemServicoRepo = ordemServicoRepo;
        this.vendaRepo = vendaRepo;
        this.pagamentoRepo = pagamentoRepo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/funcionarios")
    public String listarFuncionarios(Model model) {
        List<Usuario> todos = usuarioRepo.findAll();

        Map<Long, Double> saldosOs = new HashMap<>();
        Map<Long, Double> saldosVendas = new HashMap<>();
        Map<Long, Double> totalPagar = new HashMap<>();

        for (Usuario u : todos) {
            // 1. Lógica de O.S. (Bruto Produzido - Já Pago especificamente como OS)
            Double brutoOS = ordemServicoRepo.somarTotalOsPendentesPorTecnico(u.getId());
            double percOS = (u.getComissaoOs() != null) ? u.getComissaoOs() : 0.0;
            double comissaoOsGerada = (brutoOS != null ? brutoOS : 0.0) * percOS / 100;

            Double jaPagosOS = pagamentoRepo.somarPorFuncionarioETipo(u.getId(), "OS");
            double saldoOsLiquido = comissaoOsGerada - (jaPagosOS != null ? jaPagosOS : 0.0);

            // 2. Lógica de Vendas (Bruto Produzido - Já Pago especificamente como VENDA)
            Double brutoVendas = vendaRepo.somarTotalVendasPendentesPorVendedor(u.getId());
            double percVenda = (u.getComissaoVenda() != null) ? u.getComissaoVenda() : 0.0;
            double comissaoVendaGerada = (brutoVendas != null ? brutoVendas : 0.0) * percVenda / 100;

            Double jaPagosVenda = pagamentoRepo.somarPorFuncionarioETipo(u.getId(), "VENDA");
            double saldoVendaLiquido = comissaoVendaGerada - (jaPagosVenda != null ? jaPagosVenda : 0.0);

            // 3. Saldo Final (Soma dos dois saldos líquidos)
            double saldoTotalLiquido = saldoOsLiquido + saldoVendaLiquido;

            // Preenche os mapas garantindo que não mostre valores negativos por erro de arredondamento
            saldosOs.put(u.getId(), Math.max(0, saldoOsLiquido));
            saldosVendas.put(u.getId(), Math.max(0, saldoVendaLiquido));
            totalPagar.put(u.getId(), Math.max(0, saldoTotalLiquido));
        }

        model.addAttribute("usuarios", todos);
        model.addAttribute("saldosOs", saldosOs);
        model.addAttribute("saldosVendas", saldosVendas);
        model.addAttribute("totalPagar", totalPagar);
        model.addAttribute("pagamentos", pagamentoRepo.findTop10ByOrderByDataHoraDesc());

        return "funcionarios";
    }
}