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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
import java.util.Optional;
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
     * Exibe a tela de PDV (Vendas) filtrada pela empresa do usuário.
     */
    @GetMapping
    public String novaVenda(Model model) {
        Usuario logado = getUsuarioLogado();
        if (logado == null) return "redirect:/login";
        Long empresaId = logado.getEmpresa().getId();

        // 🔐 SEGURANÇA SaaS: Busca apenas as vendas da empresa logada
        List<Venda> todasVendas = vendaRepo.findByEmpresaIdOrderByDataHoraDesc(empresaId);

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
     * Endpoint para busca dinâmica (Ajax) - Ajustado para SaaS.
     */
    @GetMapping("/filtrar")
    @ResponseBody
    public List<Venda> filtrarVendas(
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Long id) {

        Usuario logado = getUsuarioLogado();
        if (logado == null) return new ArrayList<>();
        Long empresaId = logado.getEmpresa().getId();

        // Se filtrar por ID, verifica se pertence à empresa
        if (id != null) {
            return vendaRepo.findById(id)
                    .filter(v -> v.getEmpresa().getId().equals(empresaId))
                    .map(List::of).orElse(new ArrayList<>());
        }

        LocalDateTime dataInicio = (data != null && !data.isEmpty())
                ? LocalDate.parse(data).atStartOfDay()
                : LocalDate.now().minusYears(10).atStartOfDay();

        LocalDateTime dataFim = (data != null && !data.isEmpty())
                ? LocalDate.parse(data).atTime(LocalTime.MAX)
                : LocalDateTime.now();

        // RESOLVE O ERRO: Usa os métodos com EmpresaId que criamos no Repository
        if (vendedor != null && !vendedor.isEmpty()) {
            return vendaRepo.findByEmpresaIdAndVendedorNomeContainingIgnoreCaseAndDataHoraBetween(empresaId, vendedor, dataInicio, dataFim);
        } else {
            return vendaRepo.findByEmpresaIdAndDataHoraBetween(empresaId, dataInicio, dataFim);
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

            Usuario vendedorObj = getUsuarioLogado();
            if (vendedorObj == null) throw new RuntimeException("Sessão inválida!");

            // 🔐 VINCULA A VENDA À EMPRESA DO VENDEDOR
            venda.setEmpresa(vendedorObj.getEmpresa());
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
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

                // Validação de segurança: o produto pertence à mesma empresa da venda?
                if (!prod.getEmpresa().getId().equals(vendedorObj.getEmpresa().getId())) {
                    throw new RuntimeException("Produto inválido para esta loja!");
                }

                if (prod.getQuantidade() < item.getQuantidade()) {
                    throw new RuntimeException("Estoque insuficiente: " + prod.getNome());
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
            venda.setComissaoVendedorValor((totalCalculado * taxaAtual) / 100);

            vendaRepo.save(venda);
            ra.addFlashAttribute("sucesso", "Venda realizada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdfVenda(@PathVariable Long id) {
        try {
            Usuario logado = getUsuarioLogado();
            Venda v = vendaRepo.findById(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));

            // Segurança: Só gera PDF se a venda for da empresa do usuário
            if (!v.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, new PageSize(226, 842));
            doc.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            // Nome dinâmico baseado na empresa logada
            doc.add(new Paragraph(v.getEmpresa().getNome().toUpperCase()).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Cupom Não Fiscal").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------").setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("CUPOM #" + v.getId()).setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Data: " + v.getDataHora().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Vendedor: " + v.getVendedor().getNome()).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
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
            h.setContentDisposition(ContentDisposition.inline().filename("Venda_" + v.getId() + ".pdf").build());
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
            Usuario logado = getUsuarioLogado();
            Venda v = vendaRepo.findById(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));

            // Segurança SaaS: Validar se a venda pertence à empresa do admin
            if (!v.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                throw new RuntimeException("Acesso negado!");
            }

            // Estorno de estoque
            for (ItemVenda i : v.getItens()) {
                Produto p = i.getProduto();
                if (p != null) {
                    p.setQuantidade(p.getQuantidade() + i.getQuantidade());
                    produtoRepo.save(p);
                }
            }

            vendaRepo.delete(v);
            ra.addFlashAttribute("sucesso", "Venda #" + id + " deletada e estoque estornado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao deletar: " + e.getMessage());
        }
        return "redirect:/vendas";
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepo.findByUsername(login).orElse(null);
    }
}