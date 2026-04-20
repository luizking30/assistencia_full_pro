package com.assistencia.controller;

import com.assistencia.model.*;
import com.assistencia.repository.VendaRepository;
import com.assistencia.repository.ProdutoRepository;
import com.assistencia.repository.UsuarioRepository;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.geom.PageSize;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendas")
public class VendasController {

    private final VendaRepository vendaRepo;
    private final ProdutoRepository produtoRepo;
    private final UsuarioRepository usuarioRepo;

    public VendasController(VendaRepository vendaRepo, ProdutoRepository produtoRepo, UsuarioRepository usuarioRepo) {
        this.vendaRepo = vendaRepo;
        this.produtoRepo = produtoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    /**
     * Exibe a tela de PDV (Vendas) com TODO o histórico do período.
     * Alterado de findByDataHoraBetween para findAll() para mostrar tudo.
     */
    @GetMapping
    public String novaVenda(Model model) {
        // 🚀 MUDANÇA AQUI: Agora busca todas as vendas do banco, não só as de hoje.
        List<Venda> todasVendas = vendaRepo.findAll();

        Double totalAcumulado = todasVendas.stream()
                .filter(v -> v.getValorTotal() != null)
                .mapToDouble(Venda::getValorTotal)
                .sum();

        model.addAttribute("vendas", todasVendas);
        model.addAttribute("totalVendasHoje", totalAcumulado);

        if (!model.containsAttribute("venda")) {
            Venda novaVenda = new Venda();
            novaVenda.setItens(new ArrayList<>());
            model.addAttribute("venda", novaVenda);
        }
        return "vendas";
    }

    /**
     * Endpoint para busca dinâmica (Ajax).
     * Ajustado para retornar tudo caso os filtros estejam vazios.
     */
    @GetMapping("/filtrar")
    @ResponseBody
    public List<Venda> filtrarVendas(
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Long id) {

        if ((vendedor == null || vendedor.isEmpty()) && (data == null || data.isEmpty()) && id == null) {
            return vendaRepo.findAll();
        }

        if (id != null) {
            return vendaRepo.findById(id).map(List::of).orElse(new ArrayList<>());
        }

        LocalDateTime dataInicio = (data != null && !data.isEmpty())
                ? LocalDate.parse(data).atStartOfDay()
                : LocalDate.now().minusYears(10).atStartOfDay();

        LocalDateTime dataFim = (data != null && !data.isEmpty())
                ? LocalDate.parse(data).atTime(LocalTime.MAX)
                : LocalDateTime.now();

        if (vendedor != null && !vendedor.isEmpty()) {
            return vendaRepo.findByVendedorNomeContainingIgnoreCaseAndDataHoraBetween(vendedor, dataInicio, dataFim);
        } else {
            return vendaRepo.findByDataHoraBetween(dataInicio, dataFim);
        }
    }

    @Transactional
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("venda") Venda venda, Authentication auth, RedirectAttributes ra) {
        try {
            if (venda.getItens() == null || venda.getItens().isEmpty()) {
                ra.addFlashAttribute("erro", "O carrinho está vazio!");
                return "redirect:/vendas";
            }

            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("Você precisa estar logado para realizar uma venda!");
            }

            Usuario vendedorObj = usuarioRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Vendedor logado não encontrado!"));

            venda.setVendedor(vendedorObj);
            venda.setDataHora(LocalDateTime.now());
            venda.setPago(false);

            Double taxaAtual = (vendedorObj.getComissaoVenda() != null) ? vendedorObj.getComissaoVenda() : 0.0;
            venda.setTaxaComissaoAplicada(taxaAtual);

            double totalCalculado = 0.0;
            double custoTotalAcumulado = 0.0;

            for (ItemVenda item : venda.getItens()) {
                if (item.getProduto() == null || item.getProduto().getId() == null) continue;

                Produto prod = produtoRepo.findById(item.getProduto().getId())
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado: ID " + item.getProduto().getId()));

                if (prod.getQuantidade() < item.getQuantidade()) {
                    throw new RuntimeException("Estoque insuficiente para: " + prod.getNome());
                }

                item.setProduto(prod);
                item.setCustoUnitario(prod.getPrecoCusto() != null ? prod.getPrecoCusto() : 0.0);
                item.setVenda(venda);

                totalCalculado += item.getPrecoUnitario() * item.getQuantidade();
                custoTotalAcumulado += item.getCustoUnitario() * item.getQuantidade();

                prod.setQuantidade(prod.getQuantidade() - item.getQuantidade());
                produtoRepo.save(prod);
            }

            venda.setValorTotal(totalCalculado);
            venda.setCustoTotalEstoque(custoTotalAcumulado);

            double valorComissaoCalculada = (totalCalculado * taxaAtual) / 100;
            venda.setComissaoVendedorValor(valorComissaoCalculada);

            vendaRepo.save(venda);

            // Atualização imediata do saldo para compatibilidade com lógica de data de corte
            if (valorComissaoCalculada > 0) {
                double saldoAnterior = (vendedorObj.getSaldoVendaCalculado() != null) ? vendedorObj.getSaldoVendaCalculado() : 0.0;
                vendedorObj.setSaldoVendaCalculado(saldoAnterior + valorComissaoCalculada);
                usuarioRepo.save(vendedorObj);
            }

            ra.addFlashAttribute("sucesso", "Venda realizada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar venda: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdfVenda(@PathVariable Long id) {
        try {
            Venda v = vendaRepo.findById(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, new PageSize(226, 842));
            doc.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            doc.add(new Paragraph("SHARK ELETRÔNICOS").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Taguatinga - DF").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("CUPOM #" + v.getId()).setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Data: " + v.getDataHora().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Vendedor: " + (v.getVendedor() != null ? v.getVendedor().getNome() : "N/D")).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));

            for (ItemVenda item : v.getItens()) {
                String nomeProd = (item.getProduto() != null) ? item.getProduto().getNome() : "Produto";
                doc.add(new Paragraph(item.getQuantidade() + "x " + nomeProd).setFontSize(8));
                doc.add(new Paragraph("Sub: R$ " + String.format("%.2f", item.getQuantidade() * item.getPrecoUnitario())).setFontSize(7).setItalic());
            }

            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("TOTAL: R$ " + String.format("%.2f", v.getValorTotal())).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\nObrigado pela preferência!").setFontSize(8).setItalic().setTextAlignment(TextAlignment.CENTER));

            doc.close();

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_PDF);
            h.setContentDisposition(ContentDisposition.inline().filename("Cupom_Shark_" + v.getId() + ".pdf").build());
            return new ResponseEntity<>(out.toByteArray(), h, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Venda v = vendaRepo.findById(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));
            Usuario vendedorObj = v.getVendedor();

            if (vendedorObj != null && v.getComissaoVendedorValor() != null && v.getComissaoVendedorValor() > 0) {
                double valorEstorno = v.getComissaoVendedorValor();
                double saldoAtual = (vendedorObj.getSaldoVendaCalculado() != null) ? vendedorObj.getSaldoVendaCalculado() : 0.0;
                vendedorObj.setSaldoVendaCalculado(Math.max(0.0, saldoAtual - valorEstorno));
                usuarioRepo.save(vendedorObj);
            }

            for (ItemVenda i : v.getItens()) {
                Produto p = i.getProduto();
                if (p != null) {
                    p.setQuantidade(p.getQuantidade() + i.getQuantidade());
                    produtoRepo.save(p);
                }
            }

            vendaRepo.delete(v);
            ra.addFlashAttribute("sucesso", "Venda #" + id + " deletada: Estoque e comissão estornados!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao deletar venda: " + e.getMessage());
        }
        return "redirect:/vendas";
    }
}