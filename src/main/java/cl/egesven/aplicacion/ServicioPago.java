package cl.egesven.aplicacion;

import cl.egesven.dominio.*;
import cl.egesven.infraestructura.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

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

    public BigDecimal obtenerMonto(Connection conn, int idPedido) throws SQLException {
        String sql = "SELECT MONTO FROM PAGO WHERE ID_PEDIDO = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("MONTO");
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
