package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/ordens")
public class OrdemServicoController {

    private final OrdemServicoRepository ordemRepo;
    private final ClienteRepository clienteRepo;

    public OrdemServicoController(OrdemServicoRepository ordemRepo, ClienteRepository clienteRepo) {
        this.ordemRepo = ordemRepo;
        this.clienteRepo = clienteRepo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        List<OrdemServico> ordens;
        // Se houver busca, filtra; caso contrário, traz tudo (ou você pode limitar os últimos 50)
        if (busca != null && !busca.isEmpty()) {
            ordens = ordemRepo.findAll().stream()
                    .filter(o -> o.getClienteNome().toLowerCase().contains(busca.toLowerCase()) ||
                            o.getId().toString().equals(busca.replace("#", "")))
                    .toList();
        } else {
            ordens = ordemRepo.findAll();
        }
        model.addAttribute("ordens", ordens);
        model.addAttribute("busca", busca);
        return "ordens";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @PostMapping
    public String salvar(@RequestParam Long clienteId,
                         @RequestParam String produto,
                         @RequestParam String defeito,
                         @RequestParam String status,
                         @RequestParam Double valor,
                         RedirectAttributes ra) {
        try {
            Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

            OrdemServico os = new OrdemServico();
            os.setClienteNome(c.getNome());
            os.setClienteCpf(c.getCpf());
            os.setClienteWhatsapp(c.getWhatsapp());
            os.setProduto(produto);
            os.setDefeito(defeito);
            os.setStatus(status);
            os.setValorTotal(valor);
            os.setData(LocalDateTime.now());
            os.setCustoPeca(0.0); // Inicializa sempre com zero na criação

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
            }

            ordemRepo.save(os);
            ra.addFlashAttribute("sucesso", "OS #" + os.getId() + " gerada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/ordens";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @PostMapping("/editar-status-ajax")
    @ResponseBody
    public ResponseEntity<?> editarStatusAjax(@RequestParam Long id,
                                              @RequestParam String status,
                                              @RequestParam(defaultValue = "0") Double custoPeca) {
        try {
            OrdemServico os = ordemRepo.findById(id).orElseThrow();
            os.setStatus(status);

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
                os.setCustoPeca(custoPeca);

                // IMPORTANTE: Aqui você decide se o valorTotal no banco deve ser
                // o valor bruto ou o líquido. Para o seu dashboard funcionar como
                // "valorTotal - custoPeca", mantemos o valorTotal como o bruto cobrado.
            } else {
                // Se mudar de 'Entregue' para outro, removemos a data de entrega
                os.setDataEntrega(null);
            }

            ordemRepo.save(os);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "novoStatus", status,
                    "novoValor", os.getValorTotal(),
                    "custoPeca", os.getCustoPeca()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id) {
        try {
            OrdemServico os = ordemRepo.findById(id).orElseThrow();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);

            // Medida para impressora térmica de 80mm
            com.itextpdf.kernel.geom.PageSize pageSize = new com.itextpdf.kernel.geom.PageSize(226, 842);
            Document document = new Document(pdf, pageSize);
            document.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            document.add(new Paragraph("SHARK ELETRÔNICOS").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Brasília - DF\nWhats: (61) 99999-9999").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("ORDEM DE SERVIÇO #" + os.getId()).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Abertura: " + os.getData().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("CLIENTE: " + os.getClienteNome()).setFontSize(9));
            document.add(new Paragraph("EQUIPAMENTO: " + os.getProduto()).setFontSize(9));
            document.add(new Paragraph("DEFEITO: " + os.getDefeito()).setFontSize(8).setItalic());
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("VALOR TOTAL: R$ " + String.format("%.2f", os.getValorTotal())).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("GARANTIA DE 90 DIAS").setFontSize(7).setTextAlignment(TextAlignment.CENTER));

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline().filename("OS_" + os.getId() + ".pdf").build());

            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}