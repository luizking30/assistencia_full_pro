package com.assistencia.controller;

import com.assistencia.model.Cliente;
import com.assistencia.model.OrdemServico;
import com.assistencia.repository.ClienteRepository;
import com.assistencia.repository.OrdemServicoRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

@Controller
@RequestMapping("/ordens")
public class OrdemServicoController {

    private final OrdemServicoRepository ordemRepo;
    private final ClienteRepository clienteRepo;

    public OrdemServicoController(OrdemServicoRepository ordemRepo, ClienteRepository clienteRepo) {
        this.ordemRepo = ordemRepo;
        this.clienteRepo = clienteRepo;
    }

    // LISTAR ORDENS
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("ordens", ordemRepo.findAll());
        model.addAttribute("clientes", clienteRepo.findAll());
        return "ordens";
    }

    // SALVAR ORDEM
    @PostMapping
    public String salvar(@RequestParam Long clienteId,
                         @RequestParam String produto,
                         @RequestParam String defeito,
                         @RequestParam String status) {

        Cliente c = clienteRepo.findById(clienteId).orElseThrow();

        OrdemServico os = new OrdemServico();
        os.setClienteNome(c.getNome());
        os.setClienteCpf(c.getCpf());
        os.setClienteWhatsapp(c.getWhatsapp());

        os.setProduto(produto);
        os.setDefeito(defeito);
        os.setStatus(status);
        os.setData(LocalDate.now());

        ordemRepo.save(os);

        return "redirect:/ordens";
    }

    // BUSCAR CLIENTES POR TERMO (CPF, WhatsApp ou NOME)
    @GetMapping("/buscar-clientes")
    @ResponseBody
    public List<Cliente> buscarClientes(@RequestParam String termo) {
        List<Cliente> resultado = new ArrayList<>();

        if (termo.matches("\\d+")) {
            resultado.addAll(clienteRepo.findByCpfContaining(termo));
            resultado.addAll(clienteRepo.findByWhatsappContaining(termo));
        } else {
            resultado.addAll(clienteRepo.findByNomeContainingIgnoreCase(termo));
        }

        return resultado;
    }

    // GERAR PDF DA ORDEM
    @GetMapping("/pdf/{id}")
    @ResponseBody
    public byte[] gerarPdf(@PathVariable Long id) throws Exception {

        OrdemServico os = ordemRepo.findById(id).orElseThrow();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("===== ORDEM DE SERVIÇO ====="));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Cliente: " + os.getClienteNome()));
        document.add(new Paragraph("CPF: " + os.getClienteCpf()));
        document.add(new Paragraph("WhatsApp: " + os.getClienteWhatsapp()));
        document.add(new Paragraph("-----------------------------------"));
        document.add(new Paragraph("Produto: " + os.getProduto()));
        document.add(new Paragraph("Defeito: " + os.getDefeito()));
        document.add(new Paragraph("Status: " + os.getStatus()));
        document.add(new Paragraph("Data: " + os.getData()));

        document.close();

        return out.toByteArray();
    }
}