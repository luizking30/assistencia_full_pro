package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
import com.assistencia.model.Usuario;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;
import com.assistencia.repository.UsuarioRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UsuarioRepository usuarioRepo;

    public OrdemServicoController(OrdemServicoRepository ordemRepo, ClienteRepository clienteRepo, UsuarioRepository usuarioRepo) {
        this.ordemRepo = ordemRepo;
        this.clienteRepo = clienteRepo;
        this.usuarioRepo = usuarioRepo;
    }

    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return usuarioRepo.findByUsername(login).orElse(null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        List<OrdemServico> ordens;
        if (busca != null && !busca.isEmpty()) {
            ordens = ordemRepo.findAll(); // Ajuste conforme sua lógica de busca real
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
            // REGRA 1: Não permitir valor menor que 0
            if (valor < 0) {
                ra.addFlashAttribute("erro", "O valor da O.S. não pode ser negativo!");
                return "redirect:/ordens";
            }

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
            os.setCustoPeca(0.0);
            os.setUsuario(getUsuarioLogado());

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

            // REGRA 2: Bloquear alteração se já estiver 'Entregue'
            if ("Entregue".equalsIgnoreCase(os.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "O.S. Finalizada!"));
            }

            os.setStatus(status);

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
                os.setCustoPeca(custoPeca);
                if (os.getUsuario() == null) {
                    os.setUsuario(getUsuarioLogado());
                }
            } else {
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

    // REGRA 3: Endpoint para DELETAR O.S.
    @PreAuthorize("hasRole('ADMIN')") // Sugestão: Apenas Admin deleta
    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            if (!ordemRepo.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O.S. não encontrada.");
            }
            ordemRepo.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao deletar O.S.");
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

            com.itextpdf.kernel.geom.PageSize pageSize = new com.itextpdf.kernel.geom.PageSize(226, 842);
            Document document = new Document(pdf, pageSize);
            document.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            document.add(new Paragraph("SHARK ELETRÔNICOS").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Brasília - DF\nWhats: (61) 99999-9999").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("ORDEM DE SERVIÇO #" + os.getId()).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            String nomeVendedor = (os.getUsuario() != null) ? os.getUsuario().getNome() : "Sistema";
            document.add(new Paragraph("Vendedor: " + nomeVendedor).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
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