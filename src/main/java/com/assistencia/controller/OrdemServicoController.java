package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.model.Empresa;
import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
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
        Usuario logado = getUsuarioLogado();
        if (logado == null) return "redirect:/login";
        Long empresaId = logado.getEmpresa().getId();

        // 🔐 ISOLAMENTO SaaS: Busca apenas OS e Clientes da empresa logada
        List<OrdemServico> ordens;
        if (busca != null && !busca.isEmpty()) {
            ordens = ordemRepo.buscarSugestoesSugestivas(busca, empresaId);
        } else {
            ordens = ordemRepo.findByEmpresaIdOrderByIdDesc(empresaId);
        }

        model.addAttribute("todosClientes", clienteRepo.findByEmpresaId(empresaId));
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
            Usuario logado = getUsuarioLogado();
            if (logado == null) throw new RuntimeException("Sessão expirada");

            Cliente c = clienteRepo.findById(clienteId)
                    .filter(cli -> cli.getEmpresa().getId().equals(logado.getEmpresa().getId()))
                    .orElseThrow(() -> new RuntimeException("Cliente não pertence a esta unidade!"));

            OrdemServico os = new OrdemServico();
            os.setEmpresa(logado.getEmpresa()); // 🔐 CARIMBO SaaS
            os.setClienteNome(c.getNome());
            os.setClienteCpf(c.getCpf());
            os.setClienteWhatsapp(c.getWhatsapp());
            os.setProduto(produto);
            os.setDefeito(defeito);
            os.setStatus(status);
            os.setValorTotal(valor != null ? valor : 0.0);
            os.setData(LocalDateTime.now());
            os.setTecnico(logado);
            os.setFuncionarioAbertura(logado.getNome());
            os.setCustoPeca(0.0);

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
                os.setFuncionarioEntrega(logado.getNome());
                if (logado.getComissaoOs() != null) {
                    os.setComissaoTecnicoValor(os.getValorTotal() * (logado.getComissaoOs() / 100.0));
                }
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
            Usuario logado = getUsuarioLogado();
            OrdemServico os = ordemRepo.findById(id).orElseThrow();

            // 🔐 SEGURANÇA: Impede editar OS de outra empresa
            if (!os.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
            }

            if ("Entregue".equalsIgnoreCase(os.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "O.S. já finalizada!"));
            }

            os.setStatus(status);
            if ("Em andamento".equalsIgnoreCase(status)) {
                os.setFuncionarioAndamento(logado.getNome());
                os.setDataAndamento(LocalDateTime.now());
            }

            if ("Entregue".equalsIgnoreCase(status)) {
                os.setDataEntrega(LocalDateTime.now());
                os.setCustoPeca(custoPeca);
                os.setFuncionarioEntrega(logado.getNome());
                os.setTecnico(logado);

                double total = os.getValorTotal() != null ? os.getValorTotal() : 0.0;
                double liquido = total - custoPeca;
                if (logado.getComissaoOs() != null) {
                    os.setComissaoTecnicoValor(liquido * (logado.getComissaoOs() / 100.0));
                }
            }

            ordemRepo.save(os);
            return ResponseEntity.ok(Map.of("success", true, "novoStatus", status, "funcionario", logado.getNome()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            Usuario logado = getUsuarioLogado();
            OrdemServico os = ordemRepo.findById(id).orElseThrow();

            // 🔐 SEGURANÇA: Impede deletar OS de outra empresa
            if (!os.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            ordemRepo.delete(os);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao deletar.");
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id) {
        try {
            Usuario logado = getUsuarioLogado();
            OrdemServico os = ordemRepo.findById(id).orElseThrow();

            // 🔐 SEGURANÇA: Impede ver PDF de outra empresa
            if (!os.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, new com.itextpdf.kernel.geom.PageSize(226, 842));
            document.setMargins(10, 10, 10, 10);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
            String nomeEmpresa = os.getEmpresa().getNome().toUpperCase();

            document.add(new Paragraph(nomeEmpresa).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Assistência Técnica").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("ORDEM DE SERVIÇO #" + os.getId()).setBold().setFontSize(11).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Abertura: " + os.getData().format(fmt)).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Vendedor: " + os.getFuncionarioAbertura()).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("CLIENTE: " + os.getClienteNome()).setFontSize(9));
            document.add(new Paragraph("EQUIPAMENTO: " + os.getProduto()).setFontSize(9));
            document.add(new Paragraph("DEFEITO: " + os.getDefeito()).setFontSize(8).setItalic());
            document.add(new Paragraph("----------------------------------------").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("TOTAL: R$ " + String.format("%.2f", os.getValorTotal())).setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\nGarantia de 90 dias nas peças trocadas.").setFontSize(7).setTextAlignment(TextAlignment.CENTER));

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