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

    /**
     * Busca o usuário logado para vincular à Ordem de Serviço.
     * Sincronizado com a Model Usuario que utiliza o campo 'username'.
     */
    private Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String login = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();

        // Alterado para findByUsername para casar com a Model e o Repository corrigidos
        return usuarioRepo.findByUsername(login).orElse(null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        List<OrdemServico> ordens;
        if (busca != null && !busca.isEmpty()) {
            ordens = ordemRepo.buscarSugestoesSugestivas(busca);
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
            os.setCustoPeca(0.0);

            // Vincula o vendedor logado à nova OS
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
            os.setStatus(status);

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
                os.setCustoPeca(custoPeca);
                // Caso a OS não tenha vendedor vinculado, associa o atual na entrega
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

            // Cabeçalho Shark Eletrônicos
            document.add(new Paragraph("SHARK ELETRÔNICOS").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Brasília - DF\nWhats: (61) 99999-9999").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("ORDEM DE SERVIÇO #" + os.getId()).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            // Exibe o nome do vendedor no PDF (pega o 'nome' real do objeto Usuario)
            String nomeVendedor = (os.getUsuario() != null) ? os.getUsuario().getNome() : "Sistema";
            document.add(new Paragraph("Vendedor: " + nomeVendedor).setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Abertura: " + os.getData().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // Dados do Cliente e Equipamento
            document.add(new Paragraph("CLIENTE: " + os.getClienteNome()).setFontSize(9));
            document.add(new Paragraph("EQUIPAMENTO: " + os.getProduto()).setFontSize(9));
            document.add(new Paragraph("DEFEITO: " + os.getDefeito()).setFontSize(8).setItalic());
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            // Financeiro e Rodapé
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