package com.banduairlines.service.pagos;

import com.banduairlines.dto.pagos.FlowPaymentCreateResponse;
import com.banduairlines.dto.pagos.FlowPaymentStatusResponse;
import com.banduairlines.dto.pagos.FlowRefundResponse;

public interface FlowClient {

    FlowPaymentCreateResponse crearPago(
            String commerceOrder,
            String subject,
            Integer amount,
            String email,
            String urlConfirmation,
            String urlReturn,
            Long timeoutSeconds
    );

    FlowPaymentStatusResponse obtenerEstadoPago(String token);

    FlowRefundResponse crearReembolso(
            String refundCommerceOrder,
            String receiverEmail,
            Integer amount,
            String urlCallBack,
            Long flowTrxId
    );

    FlowRefundResponse obtenerEstadoReembolso(String token);
}
