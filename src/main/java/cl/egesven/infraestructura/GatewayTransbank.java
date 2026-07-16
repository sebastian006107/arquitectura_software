package cl.egesven.infraestructura;

import cl.egesven.dominio.Pago;

import java.math.BigDecimal;
import java.util.Random;

public class GatewayTransbank {

    private final Random random = new Random();

    public Pago procesarPago(int idPedido, int idMedioPago, BigDecimal monto) {
        boolean aprobado = random.nextDouble() < 0.8;
        String token = "TBK-" + System.currentTimeMillis() + "-" + random.nextInt(10000);

        System.out.println("[SIMULACION Transbank] Procesando pago por $" + monto + "...");
        System.out.println("[SIMULACION Transbank] Token: " + token);

        Pago pago = new Pago();
        pago.setIdPedido(idPedido);
        pago.setIdMedioPago(idMedioPago);
        pago.setMonto(monto);
        pago.setTokenTransaccion(token);
        pago.setEstado(aprobado ? "APROBADO" : "RECHAZADO");

        if (aprobado) {
            System.out.println("[SIMULACION Transbank] Pago APROBADO");
        } else {
            System.out.println("[SIMULACION Transbank] Pago RECHAZADO");
        }

        return pago;
    }
}
