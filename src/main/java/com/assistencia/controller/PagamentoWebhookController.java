package com.assistencia.controller;

import com.assistencia.model.Empresa;
import com.assistencia.repository.EmpresaRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/webhook")
public class PagamentoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoWebhookController.class);

    @Autowired
    private EmpresaRepository empresaRepo;

    @Value("${mercado_pago_sample_access_token}")
    private String mpAccessToken;

    /**
     * Endpoint que o Mercado Pago chama automaticamente.
     * URL para configurar no painel: https://seu-dominio.ngrok-free.app/api/webhook/pagamento
     */
    @PostMapping("/pagamento")
    public ResponseEntity<Void> receberNotificacao(
            @RequestParam(value = "data.id", required = false) Long dataId,
            @RequestParam(value = "type", required = false) String type) {

        // O Mercado Pago envia notificações de vários tipos. Queremos apenas 'payment'.
        if ("payment".equals(type) && dataId != null) {

            MercadoPagoConfig.setAccessToken(mpAccessToken);
            PaymentClient client = new PaymentClient();

            try {
                // 1. Buscamos o pagamento na API do Mercado Pago para confirmar se é real
                Payment payment = client.get(dataId);

                // 2. Verificamos se o status é 'approved' (Aprovado)
                if ("approved".equals(payment.getStatus())) {

                    // 3. Pegamos a referência que enviamos: "ID_EMPRESA:DIAS"
                    String reference = payment.getExternalReference();

                    if (reference != null && reference.contains(":")) {
                        String[] partes = reference.split(":");
                        Long empresaId = Long.parseLong(partes[0]);
                        int diasComprados = Integer.parseInt(partes[1]);

                        // 4. Atualizamos o banco de dados da Shark Eletrônicos
                        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);

                        if (empresa != null) {
                            int saldoAtual = (empresa.getDiasRestantes() != null) ? empresa.getDiasRestantes() : 0;
                            empresa.setDiasRestantes(saldoAtual + diasComprados);

                            empresaRepo.save(empresa);
                            logger.info("PAGAMENTO APROVADO: Empresa {} recebeu {} dias.", empresa.getNome(), diasComprados);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao processar Webhook do Mercado Pago: ", e);
                // Retornamos 500 para o MP tentar enviar a notificação novamente mais tarde
                return ResponseEntity.internalServerError().build();
            }
        }

        // Retornamos 200 ou 201 para dizer ao Mercado Pago que recebemos o aviso
        return ResponseEntity.ok().build();
    }
}