package cl.egesven.aplicacion;

import cl.egesven.dominio.*;
import cl.egesven.infraestructura.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class ServicioPago {

    private final GatewayTransbank gatewayTransbank;
    private final GatewayPaypal gatewayPaypal;

    public ServicioPago() {
        this.gatewayTransbank = new GatewayTransbank();
        this.gatewayPaypal = new GatewayPaypal();
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
        String sql = "SELECT ID_MEDIO_PAGO FROM MEDIO_PAGO WHERE TIPO = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = Conexion.getInstancia().getConexion();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo.toUpperCase());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ID_MEDIO_PAGO");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar medio de pago: " + e.getMessage(), e);
        }
        throw new RuntimeException("Medio de pago no encontrado o inactivo: " + tipo);
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
