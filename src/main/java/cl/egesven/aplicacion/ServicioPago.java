package cl.egesven.aplicacion;

import cl.egesven.dominio.*;
import cl.egesven.infraestructura.*;

import java.math.BigDecimal;

public class ServicioPago {

    private final GatewayTransbank gatewayTransbank;
    private final GatewayPaypal gatewayPaypal;
    private final RepositorioPedidos repositorioPedidos;

    public ServicioPago() {
        this.gatewayTransbank = new GatewayTransbank();
        this.gatewayPaypal = new GatewayPaypal();
        this.repositorioPedidos = new RepositorioPedidos();
    }

    public Pago procesarPago(int idPedido, int idMedioPago, BigDecimal monto, String tipoMedioPago) {
        if ("TRANSBANK".equalsIgnoreCase(tipoMedioPago)) {
            return gatewayTransbank.procesarPago(idPedido, idMedioPago, monto);
        } else if ("PAYPAL".equalsIgnoreCase(tipoMedioPago)) {
            return gatewayPaypal.procesarPago(idPedido, idMedioPago, monto);
        } else {
            throw new RuntimeException("Medio de pago no soportado: " + tipoMedioPago);
        }
    }

    public int obtenerIdMedioPagoPorTipo(String tipo) {
        return repositorioPedidos.buscarIdMedioPago(tipo);
    }

}
