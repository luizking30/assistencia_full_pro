package com.assistencia.controller;

import com.assistencia.model.ItemVenda;
import com.assistencia.model.Produto;
import com.assistencia.model.Venda;
import com.assistencia.repository.ProdutoRepository;
import com.assistencia.repository.VendaRepository;

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

@Controller
@RequestMapping("/vendas")
public class VendasController {

    private final VendaRepository vendaRepo;
    private final ProdutoRepository produtoRepo;

    public VendasController(VendaRepository vendaRepo, ProdutoRepository produtoRepo) {
        this.vendaRepo = vendaRepo;
        this.produtoRepo = produtoRepo;
    }

    @GetMapping
    public String novaVenda(Model model) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDia = LocalDate.now().atTime(LocalTime.MAX);
        List<Venda> vendasHoje = vendaRepo.findByDataHoraBetween(inicioDia, fimDia);

        Double totalVendasHoje = vendasHoje.stream()
                .filter(v -> v.getValorTotal() != null)
                .mapToDouble(Venda::getValorTotal)
                .sum();

        model.addAttribute("vendas", vendasHoje);
        model.addAttribute("totalVendasHoje", totalVendasHoje);

        if (!model.containsAttribute("venda")) {
            Venda novaVenda = new Venda();
            novaVenda.setItens(new ArrayList<>());
            model.addAttribute("venda", novaVenda);
        }
        return "vendas";
    }

    @Transactional
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("venda") Venda venda, Authentication auth, RedirectAttributes ra) {
        try {
            if (venda.getItens() == null || venda.getItens().isEmpty()) {
                ra.addFlashAttribute("erro", "O carrinho está vazio!");
                return "redirect:/vendas";
            }

            venda.setVendedor(auth != null ? auth.getName() : "Sistema Shark");

            for (ItemVenda item : venda.getItens()) {
                if (item.getProduto() == null || item.getProduto().getId() == null) continue;

                Produto prod = produtoRepo.findById(item.getProduto().getId())
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

                // 1. Validação de Estoque
                if (prod.getQuantidade() < item.getQuantidade()) {
                    throw new RuntimeException("Estoque insuficiente para: " + prod.getNome());
                }

                // 2. Validação de Desconto (Limite Shark de 10%)
                if (item.getDesconto() != null && item.getDesconto() > 10.1) {
                    throw new RuntimeException("Desconto acima do permitido!");
                }

                // --- 🚀 CORREÇÃO DO SÍMBOLO ---
                // Seta o custo unitário usando o nome correto do campo na Model Produto (precoCusto)
                item.setCustoUnitario(prod.getPrecoCusto());

                item.setVenda(venda);

                // 3. Baixa no estoque
                prod.setQuantidade(prod.getQuantidade() - item.getQuantidade());
                produtoRepo.save(prod);
            }

            // O cálculo de valorTotal e custoTotalEstoque ocorre no @PrePersist da classe Venda
            vendaRepo.save(venda);

            ra.addFlashAttribute("sucesso", "Venda realizada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdfVenda(@PathVariable Long id) {
        try {
            Venda v = vendaRepo.findById(id).orElseThrow();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, new PageSize(226, 842));
            doc.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            doc.add(new Paragraph("SHARK ELETRÔNICOS").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Brasília - DF").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("CUPOM #" + v.getId()).setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Data: " + v.getDataHora().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));

            for (ItemVenda item : v.getItens()) {
                doc.add(new Paragraph(item.getQuantidade() + "x " + item.getProduto().getNome()).setFontSize(8));
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
    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Venda v = vendaRepo.findById(id).orElseThrow();
            v.getItens().forEach(i -> {
                Produto p = i.getProduto();
                if (p != null) {
                    p.setQuantidade(p.getQuantidade() + i.getQuantidade());
                    produtoRepo.save(p);
                }
            });
            vendaRepo.delete(v);
            ra.addFlashAttribute("sucesso", "Venda estornada e estoque devolvido!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao deletar venda.");
        }
        return "redirect:/vendas";
    }
}