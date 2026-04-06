package com.assistencia.controller;

import com.assistencia.model.OrdemServico;
import com.assistencia.model.PagamentoComissao;
import com.assistencia.model.Usuario;
import com.assistencia.model.Empresa;
import com.assistencia.model.Venda;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.PagamentoComissaoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.repository.VendaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class GanhosController {

    private final VendaRepository vendaRepo;
    private final OrdemServicoRepository ordemRepo;
    private final UsuarioRepository usuarioRepo;
    private final PagamentoComissaoRepository pagamentoRepo;

    public GanhosController(VendaRepository vendaRepo, OrdemServicoRepository ordemRepo,
                            UsuarioRepository usuarioRepo, PagamentoComissaoRepository pagamentoRepo) {
        this.vendaRepo = vendaRepo;
        this.ordemRepo = ordemRepo;
        this.usuarioRepo = usuarioRepo;
        this.pagamentoRepo = pagamentoRepo;
    }
    @GetMapping("/meus-ganhos")
    public String meusGanhos(Model model, Authentication auth) {
        Usuario usuarioLogado = usuarioRepo.findByUsername(auth.getName()).orElse(null);
        if (usuarioLogado == null) return "redirect:/login";

        final Long idU = usuarioLogado.getId();
        final String nomeU = (usuarioLogado.getNome() != null) ? usuarioLogado.getNome().trim() : "";

        // 1. PEGAR A DATA DO ÚLTIMO PAGAMENTO (MARCO ZERO)
        List<PagamentoComissao> histórico = pagamentoRepo.findByFuncionarioIdOrderByDataHoraDesc(idU);

        LocalDateTime corteOs = histórico.stream()
                .filter(p -> "OS".equals(p.getTipoComissao()))
                .map(PagamentoComissao::getDataHora).findFirst().orElse(LocalDateTime.of(2000,1,1,0,0));

        LocalDateTime corteVenda = histórico.stream()
                .filter(p -> "VENDA".equals(p.getTipoComissao()))
                .map(PagamentoComissao::getDataHora).findFirst().orElse(LocalDateTime.of(2000,1,1,0,0));

        // 2. FILTRAR VENDAS SÓ APÓS O ÚLTIMO PAGAMENTO
        List<Venda> vendasNovas = vendaRepo.findAll().stream()
                .filter(v -> (v.getVendedor() != null && Objects.equals(v.getVendedor().getId(), idU)) ||
                        (v.getNomeVendedorNoAto() != null && v.getNomeVendedorNoAto().equalsIgnoreCase(nomeU)))
                .filter(v -> v.getDataHora() != null && v.getDataHora().isAfter(corteVenda))
                .collect(Collectors.toList());

        BigDecimal comissaoVenda = vendasNovas.stream()
                .map(v -> BigDecimal.valueOf(v.getComissaoVendedorValor() != null ? v.getComissaoVendedorValor() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal brutoVenda = vendasNovas.stream()
                .map(v -> BigDecimal.valueOf(v.getValorTotal() != null ? v.getValorTotal() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. FILTRAR OS SÓ APÓS O ÚLTIMO PAGAMENTO
        List<OrdemServico> servicosNovos = ordemRepo.findAll().stream()
                .filter(os -> ("Entregue".equalsIgnoreCase(os.getStatus())) &&
                        os.getFuncionarioAndamento() != null &&
                        os.getFuncionarioAndamento().trim().equalsIgnoreCase(nomeU))
                .filter(os -> os.getDataEntrega() != null && os.getDataEntrega().isAfter(corteOs))
                .collect(Collectors.toList());

        BigDecimal comissaoOs = servicosNovos.stream()
                .map(os -> {
                    if (os.getComissaoTecnicoValor() != null && os.getComissaoTecnicoValor() > 0) {
                        return BigDecimal.valueOf(os.getComissaoTecnicoValor());
                    }
                    BigDecimal liq = BigDecimal.valueOf(os.getValorTotal() - (os.getCustoPeca() != null ? os.getCustoPeca() : 0.0));
                    BigDecimal taxa = BigDecimal.valueOf(usuarioLogado.getComissaoOs() != null ? usuarioLogado.getComissaoOs() : 0.0);
                    return liq.multiply(taxa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal brutoOs = servicosNovos.stream()
                .map(os -> BigDecimal.valueOf(os.getValorTotal() != null ? os.getValorTotal() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. ATUALIZA INTERFACE
        usuarioLogado.setBrutoVendaCalculado(brutoVenda.doubleValue());
        usuarioLogado.setBrutoOsCalculado(brutoOs.doubleValue());
        usuarioLogado.setSaldoVendaCalculado(comissaoVenda.doubleValue());
        usuarioLogado.setTotalComissaoOsAcumulada(comissaoOs.doubleValue());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("vendas", vendasNovas);
        model.addAttribute("servicos", servicosNovos);
        model.addAttribute("meusPagamentos", histórico);
        model.addAttribute("totalGeral", comissaoVenda.add(comissaoOs).doubleValue());
        model.addAttribute("dataUltimaOs", corteOs.getYear() < 2010 ? "--" : corteOs.format(fmt));
        model.addAttribute("dataUltimaVenda", corteVenda.getYear() < 2010 ? "--" : corteVenda.format(fmt));

        return "ganhos";
    }
}