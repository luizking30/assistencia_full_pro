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
            // 1. Validação de segurança dos parâmetros
            if (quantidadeDias <= 0) {
                ra.addFlashAttribute("mensagemErro", "Quantidade de dias inválida.");
                return "redirect:/pagamento";
            }

            if (mpAccessToken == null || mpAccessToken.isBlank()) {
                throw new RuntimeException("Token de acesso não configurado no servidor.");
            }

            // 2. Configura Mercado Pago
            MercadoPagoConfig.setAccessToken(mpAccessToken.trim());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = usuarioRepo.findByUsername(auth.getName()).orElse(null);

            if (usuario == null || usuario.getEmpresa() == null) {
                ra.addFlashAttribute("mensagemErro", "Perfil de usuário incompleto.");
                return "redirect:/pagamento";
            }

            // 3. Validação de dados do pagador
            String email = usuario.getEmail();
            String cpfLimpado = limparCpf(usuario.getCpf());

            if (email == null || email.isBlank() || cpfLimpado == null || cpfLimpado.length() != 11) {
                ra.addFlashAttribute("mensagemErro", "Dados cadastrais incompletos. Verifique seu CPF e E-mail.");
                return "redirect:/pagamento";
            }

            // 4. Lógica de negócio (Custo de R$ 2,00 por dia)
            BigDecimal valorTotal = BigDecimal.valueOf(quantidadeDias)
                    .multiply(new BigDecimal("2.00"));

            System.out.println("===== INICIANDO GERAÇÃO DE PIX =====");
            System.out.println("Empresa: " + usuario.getEmpresa().getNome());
            System.out.println("Valor: R$ " + valorTotal);

            PaymentClient client = new PaymentClient();

            // 5. Construção do Payer
            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(email)
                    .firstName(usuario.getNome())
                    .identification(
                            IdentificationRequest.builder()
                                    .type("CPF")
                                    .number(cpfLimpado)
                                    .build()
                    )
                    .build();

            // 6. Requisição do Pagamento
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorTotal)
                    .description("Recarga Shark Eletrônicos: " + quantidadeDias + " dias")
                    .paymentMethodId("pix")
                    // external_reference é fundamental para o Webhook saber qual empresa liberar
                    .externalReference(usuario.getEmpresa().getId() + ":" + quantidadeDias)
                    .payer(payer)
                    .build();

            Payment payment = client.create(paymentRequest);

            if (payment == null || payment.getPointOfInteraction() == null) {
                throw new RuntimeException("Mercado Pago não retornou dados de interação.");
            }

            // 7. Sucesso - Enviando dados para o Thymeleaf
            model.addAttribute("copiaECola",
                    payment.getPointOfInteraction().getTransactionData().getQrCode());
            model.addAttribute("qrCodeBase64",
                    payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
            model.addAttribute("diasComprados", quantidadeDias);
            model.addAttribute("valorPago", valorTotal);

            return "pagamento_pix";

        } catch (MPApiException e) {
            // LOG DE SERVIDOR (O que você vê no console)
            System.err.println("===== ERRO API MERCADO PAGO =====");
            System.err.println("Status HTTP: " + e.getApiResponse().getStatusCode());
            System.err.println("JSON Erro: " + e.getApiResponse().getContent());

            // MENSAGEM PARA O USUÁRIO (Segura e amigável)
            ra.addFlashAttribute("mensagemErro",
                    "Não foi possível processar o Pix com o Mercado Pago. Verifique se o seu CPF é válido.");
            return "redirect:/pagamento";

        } catch (Exception e) {
            // LOG DE ERRO GENÉRICO
            e.printStackTrace();
            ra.addFlashAttribute("mensagemErro",
                    "Ocorreu um erro interno ao gerar o pagamento. Tente novamente em instantes.");
            return "redirect:/pagamento";
        }
    }

    private String limparCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}