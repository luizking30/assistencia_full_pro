package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
        if (busca != null && !busca.isEmpty()) {
            String termoLimpo = busca.replace("#", "").trim();
            ordens = ordemRepo.buscarSugestoesSugestivas(termoLimpo);
        } else {
            ordens = ordemRepo.findAll();
        }
        model.addAttribute("ordens", ordens);
        model.addAttribute("clientes", clienteRepo.findAll());
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
            Cliente c = clienteRepo.findById(clienteId).orElseThrow();
            OrdemServico os = new OrdemServico();
            os.setClienteNome(c.getNome());
            os.setClienteCpf(c.getCpf());
            os.setClienteWhatsapp(c.getWhatsapp());
            os.setProduto(produto);
            os.setDefeito(defeito);
            os.setStatus(status);
            os.setValorTotal(valor);
            os.setData(LocalDateTime.now());
            if ("Entregue".equalsIgnoreCase(status)) os.setDataEntrega(LocalDateTime.now());
            ordemRepo.save(os);
            ra.addFlashAttribute("sucesso", "OS #" + os.getId() + " gerada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/ordens";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id) {
        try {
            OrdemServico os = ordemRepo.findById(id).orElseThrow();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);

            // Configuração para 80mm (226 pontos)
            com.itextpdf.kernel.geom.PageSize pageSize = new com.itextpdf.kernel.geom.PageSize(226, 842);
            Document document = new Document(pdf, pageSize);
            document.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            // --- CABEÇALHO CENTRALIZADO ---
            document.add(new Paragraph("SHARK ELETRÔNICOS")
                    .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("CNPJ: 00.000.000/0001-00\n Rua Exemplo, 123 - Centro\n Whats: (00) 00000-0000")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // --- INFO DA ORDEM ---
            document.add(new Paragraph("ORDEM DE SERVIÇO\n#" + os.getId())
                    .setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Abertura: " + os.getData().format(fmt))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // --- DADOS DO CLIENTE CENTRALIZADOS ---
            document.add(new Paragraph("CLIENTE")
                    .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(os.getClienteNome() + "\nCPF: " + os.getClienteCpf() + "\nFone: " + os.getClienteWhatsapp())
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("-").setFontSize(5).setTextAlignment(TextAlignment.CENTER)); // Espaçador pequeno

            // --- DETALHES DO EQUIPAMENTO CENTRALIZADOS ---
            document.add(new Paragraph("EQUIPAMENTO")
                    .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(os.getProduto())
                    .setFontSize(9).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("DEFEITO")
                    .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(os.getDefeito())
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // --- VALOR E STATUS ---
            document.add(new Paragraph("STATUS: " + os.getStatus().toUpperCase())
                    .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER));

            Double v = os.getValorTotal() != null ? os.getValorTotal() : 0.0;
            document.add(new Paragraph("VALOR TOTAL: R$ " + String.format("%.2f", v))
                    .setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // --- RODAPÉ E ASSINATURA ---
            document.add(new Paragraph("GARANTIA DE 90 DIAS\nNão cobrimos mau uso ou lacre rompido.")
                    .setFontSize(7).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n__________________________\nAssinatura do Cliente")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(10));

            document.add(new Paragraph("\nObrigado pela preferência!")
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("OS_Shark_Termica_" + os.getId() + ".pdf").build());

            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper para criar células padronizadas
    private Cell createCell(String label, String value) {
        Paragraph p = new Paragraph().add(new Text(label + ": ").setBold().setFontSize(8))
                .add(new Text(value != null ? value : "-").setFontSize(10));
        return new Cell().add(p).setPadding(5).setBorder(new SolidBorder(0.5f));
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/sugestoes")
    @ResponseBody
    public List<Map<String, String>> sugestoes(@RequestParam String termo) {
        List<Map<String, String>> resultados = new ArrayList<>();
        if (termo != null && termo.length() >= 1) {
            String termoLimpo = termo.replace("#", "").trim();
            ordemRepo.buscarSugestoesSugestivas(termoLimpo).stream()
                    .limit(10)
                    .forEach(o -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", o.getId().toString());
                        map.put("cliente", o.getClienteNome());
                        map.put("produto", o.getProduto());
                        resultados.add(map);
                    });
        }
        return resultados;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @PostMapping("/editar-status-ajax")
    @ResponseBody
    public Map<String, Object> atualizarStatusAjax(@RequestParam Long id, @RequestParam String status) {
        OrdemServico os = ordemRepo.findById(id).orElseThrow();
        os.setStatus(status);
        if ("Entregue".equalsIgnoreCase(status)) os.setDataEntrega(LocalDateTime.now());
        ordemRepo.save(os);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("novoStatus", status);
        return res;
    }
}