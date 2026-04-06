package com.assistencia.controller;

import com.assistencia.model.Usuario;
import com.assistencia.repository.UsuarioRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.exceptions.MPApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class DiasPagoController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Value("${mercado_pago_sample_access_token}")
    private String mpAccessToken;

    @GetMapping("/pagamento")
    public String exibirPagamento(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            usuarioRepo.findByUsername(auth.getName()).ifPresent(u -> {
                model.addAttribute("usuario", u);
                model.addAttribute("empresa", u.getEmpresa());
            });
        }
        return "pagamento";
    }

    @PostMapping("/pagamento/gerar-pix")
    public String processarPagamentoPix(
            @RequestParam("quantidadeDias") int quantidadeDias,
            Model model,
            RedirectAttributes ra) {

        try {
            // Validação básica
            if (quantidadeDias <= 0) {
                ra.addFlashAttribute("mensagemErro", "Quantidade de dias inválida.");
                return "redirect:/pagamento";
            }

            if (mpAccessToken == null || mpAccessToken.isBlank()) {
                throw new RuntimeException("Access Token não configurado.");
            }

            // Configura token Mercado Pago
            MercadoPagoConfig.setAccessToken(mpAccessToken.trim());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = usuarioRepo.findByUsername(auth.getName()).orElse(null);

            if (usuario == null || usuario.getEmpresa() == null) {
                ra.addFlashAttribute("mensagemErro", "Usuário ou empresa não encontrados.");
                return "redirect:/pagamento";
            }

            if (usuario.getEmail() == null || usuario.getCpf() == null) {
                ra.addFlashAttribute("mensagemErro", "Usuário sem email ou CPF cadastrados.");
                return "redirect:/pagamento";
            }

            // Calcula valor total
            BigDecimal valorTotal = BigDecimal.valueOf(quantidadeDias)
                    .multiply(new BigDecimal("2.00"));

            System.out.println("===== GERANDO PIX =====");
            System.out.println("VALOR: " + valorTotal);

            PaymentClient client = new PaymentClient();

            // Payer com CPF e email real
            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(usuario.getEmail())
                    .firstName(usuario.getNome())
                    .identification(
                            IdentificationRequest.builder()
                                    .type("CPF")
                                    .number(limparCpf(usuario.getCpf()))
                                    .build()
                    )
                    .build();

            // Criar pagamento PIX
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorTotal)
                    .description("Recarga: " + quantidadeDias + " dias")
                    .paymentMethodId("pix")
                    .externalReference(usuario.getEmpresa().getId() + ":" + quantidadeDias)
                    .payer(payer)
                    .build();

            Payment payment = client.create(paymentRequest);

            if (payment == null || payment.getPointOfInteraction() == null) {
                throw new RuntimeException("Erro ao gerar pagamento PIX.");
            }

            // Retorno do QR Code
            model.addAttribute("copiaECola",
                    payment.getPointOfInteraction().getTransactionData().getQrCode());
            model.addAttribute("qrCodeBase64",
                    payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
            model.addAttribute("diasComprados", quantidadeDias);
            model.addAttribute("valorPago", valorTotal);

            return "pagamento_pix";

        } catch (MPApiException e) {
            System.err.println("===== ERRO MERCADO PAGO =====");
            System.err.println("Status: " + e.getApiResponse().getStatusCode());
            System.err.println("Resposta: " + e.getApiResponse().getContent());

            ra.addFlashAttribute("mensagemErro",
                    "Erro MP: " + e.getApiResponse().getContent());
            return "redirect:/pagamento";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("mensagemErro",
                    "Erro interno: " + e.getMessage());
            return "redirect:/pagamento";
        }
    }

    // Função utilitária para limpar CPF (somente números)
    private String limparCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}