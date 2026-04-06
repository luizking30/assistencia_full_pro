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
     * URL para configurar no painel do Mercado Pago:
     * https://gestaoshark.up.railway.app/api/webhook/pagamento
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
                // 1. Buscamos o pagamento na API do Mercado Pago para confirmar se é real e seguro
                Payment payment = client.get(dataId);

                // 2. Verificamos se o status retornado pela API oficial é 'approved'
                if ("approved".equals(payment.getStatus())) {

                    // 3. Pegamos a referência que enviamos na geração do Pix: "ID_EMPRESA:DIAS"
                    String reference = payment.getExternalReference();

                    if (reference != null && reference.contains(":")) {
                        String[] partes = reference.split(":");
                        Long empresaId = Long.parseLong(partes[0]);
                        int diasComprados = Integer.parseInt(partes[1]);

                        // 4. Localizamos a empresa no banco de dados da Shark
                        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);

                        if (empresa != null) {
                            // 5. Calculamos o novo saldo de dias e salvamos
                            int saldoAtual = (empresa.getDiasRestantes() != null) ? empresa.getDiasRestantes() : 0;
                            empresa.setDiasRestantes(saldoAtual + diasComprados);

                            empresaRepo.save(empresa);

                            logger.info("✅ SUCESSO NO WEBHOOK: Empresa {} (ID: {}) recebeu +{} dias.",
                                    empresa.getNome(), empresaId, diasComprados);
                        } else {
                            logger.warn("⚠️ WEBHOOK: Empresa com ID {} não encontrada no banco.", empresaId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("❌ ERRO GRAVE no Webhook do Mercado Pago: ", e);
                // Retornamos 500 para o MP entender que deve tentar reenviar a notificação mais tarde
                return ResponseEntity.internalServerError().build();
            }
        }

        // Retornamos 200 OK para o Mercado Pago parar de enviar esta notificação específica
        return ResponseEntity.ok().build();
    }
}