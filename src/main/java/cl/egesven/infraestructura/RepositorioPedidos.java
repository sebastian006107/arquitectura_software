package cl.egesven.infraestructura;

import cl.egesven.dominio.DetallePedido;
import cl.egesven.dominio.Pago;
import cl.egesven.dominio.Pedido;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositorioPedidos {

    public int crearPedido(Connection conn, Pedido pedido) throws SQLException {
        String sql = "INSERT INTO PEDIDO (ID_CLIENTE, FECHA, ESTADO, DIRECCION_ENVIO, " +
                     "SUBTOTAL, IMPUESTO, COSTO_ENVIO, TOTAL) " +
                     "VALUES (?, SYSDATE, ?, ?, ?, ?, ?, ?)";
        String[] cols = {"ID_PEDIDO"};
        try (PreparedStatement ps = conn.prepareStatement(sql, cols)) {
            ps.setInt(1, pedido.getIdCliente());
            ps.setString(2, pedido.getEstado());
            ps.setString(3, pedido.getDireccionEnvio());
            ps.setBigDecimal(4, pedido.getSubtotal());
            ps.setBigDecimal(5, pedido.getImpuesto());
            ps.setBigDecimal(6, pedido.getCostoEnvio());
            ps.setBigDecimal(7, pedido.getTotal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID del pedido");
    }

    public void crearDetallePedido(Connection conn, DetallePedido detalle) throws SQLException {
        String sql = "INSERT INTO DETALLE_PEDIDO (ID_PEDIDO, ID_PRODUCTO, CANTIDAD, " +
                     "PRECIO_UNITARIO, SUBTOTAL) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalle.getIdPedido());
            ps.setInt(2, detalle.getIdProducto());
            ps.setInt(3, detalle.getCantidad());
            ps.setBigDecimal(4, detalle.getPrecioUnitario());
            ps.setBigDecimal(5, detalle.getSubtotal());
            ps.executeUpdate();
        }
    }

    public void descontarStock(Connection conn, int idProducto, int cantidad) throws SQLException {
        String sql = "UPDATE PRODUCTO SET STOCK = STOCK - ? WHERE ID_PRODUCTO = ? AND STOCK >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("Stock insuficiente para el producto ID " + idProducto);
            }
        }
    }

    public void registrarPago(Connection conn, Pago pago) throws SQLException {
        String sql = "INSERT INTO PAGO (ID_PEDIDO, ID_MEDIO_PAGO, TOKEN_TRANSACCION, " +
                     "MONTO, FECHA, ESTADO) VALUES (?, ?, ?, ?, SYSDATE, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pago.getIdPedido());
            ps.setInt(2, pago.getIdMedioPago());
            ps.setString(3, pago.getTokenTransaccion());
            ps.setBigDecimal(4, pago.getMonto());
            ps.setString(5, pago.getEstado());
            ps.executeUpdate();
        }
    }

    /**
     * Cambia el estado dentro de la transaccion de compra en curso (no hace commit:
     * lo confirma quien abrio la transaccion).
     */
    public void actualizarEstado(Connection conn, int idPedido, String nuevoEstado) throws SQLException {
        String sql = "UPDATE PEDIDO SET ESTADO = ? WHERE ID_PEDIDO = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    /** Datos del pago asociados al pedido; alimentan el recibo (RF010). */
    public Pago buscarPagoPorPedido(int idPedido) {
        String sql = "SELECT g.ID_PAGO, g.ID_PEDIDO, g.ID_MEDIO_PAGO, g.TOKEN_TRANSACCION, " +
                     "g.MONTO, g.ESTADO, m.TIPO " +
                     "FROM PAGO g JOIN MEDIO_PAGO m ON g.ID_MEDIO_PAGO = m.ID_MEDIO_PAGO " +
                     "WHERE g.ID_PEDIDO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pago pago = new Pago();
                    pago.setIdPago(rs.getInt("ID_PAGO"));
                    pago.setIdPedido(rs.getInt("ID_PEDIDO"));
                    pago.setIdMedioPago(rs.getInt("ID_MEDIO_PAGO"));
                    pago.setTokenTransaccion(rs.getString("TOKEN_TRANSACCION"));
                    pago.setMonto(rs.getBigDecimal("MONTO"));
                    pago.setEstado(rs.getString("ESTADO"));
                    pago.setTipoMedioPago(rs.getString("TIPO"));
                    return pago;
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar pago del pedido: " + idPedido, e);
            throw new RuntimeException("Error al buscar pago: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Pedido> listarPedidosPorCliente(int idCliente) {
        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT ID_PEDIDO, ID_CLIENTE, FECHA, ESTADO, DIRECCION_ENVIO, " +
                     "SUBTOTAL, IMPUESTO, COSTO_ENVIO, TOTAL " +
                     "FROM PEDIDO WHERE ID_CLIENTE = ? ORDER BY FECHA DESC";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapearPedido(rs));
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al listar pedidos del cliente: " + idCliente, e);
            throw new RuntimeException("Error al listar pedidos: " + e.getMessage(), e);
        }
        return pedidos;
    }

    public Pedido buscarPedidoPorId(int idPedido) {
        String sql = "SELECT ID_PEDIDO, ID_CLIENTE, FECHA, ESTADO, DIRECCION_ENVIO, " +
                     "SUBTOTAL, IMPUESTO, COSTO_ENVIO, TOTAL " +
                     "FROM PEDIDO WHERE ID_PEDIDO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pedido p = mapearPedido(rs);
                    p.setDetalles(obtenerDetalles(idPedido));
                    return p;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pedido: " + e.getMessage(), e);
        }
        return null;
    }

    public List<DetallePedido> obtenerDetalles(int idPedido) {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT d.ID_DETALLE, d.ID_PEDIDO, d.ID_PRODUCTO, d.CANTIDAD, " +
                     "d.PRECIO_UNITARIO, d.SUBTOTAL, p.NOMBRE AS NOMBRE_PRODUCTO " +
                     "FROM DETALLE_PEDIDO d JOIN PRODUCTO p ON d.ID_PRODUCTO = p.ID_PRODUCTO " +
                     "WHERE d.ID_PEDIDO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setIdDetalle(rs.getInt("ID_DETALLE"));
                    d.setIdPedido(rs.getInt("ID_PEDIDO"));
                    d.setIdProducto(rs.getInt("ID_PRODUCTO"));
                    d.setCantidad(rs.getInt("CANTIDAD"));
                    d.setPrecioUnitario(rs.getBigDecimal("PRECIO_UNITARIO"));
                    d.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
                    d.setNombreProducto(rs.getString("NOMBRE_PRODUCTO"));
                    detalles.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener detalles: " + e.getMessage(), e);
        }
        return detalles;
    }

    public List<Pedido> listarTodosPedidos() {
        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT ID_PEDIDO, ID_CLIENTE, FECHA, ESTADO, DIRECCION_ENVIO, " +
                     "SUBTOTAL, IMPUESTO, COSTO_ENVIO, TOTAL " +
                     "FROM PEDIDO ORDER BY FECHA DESC";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pedidos.add(mapearPedido(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar todos los pedidos: " + e.getMessage(), e);
        }
        return pedidos;
    }

    public void actualizarEstado(int idPedido, String nuevoEstado) {
        String sql = "UPDATE PEDIDO SET ESTADO = ? WHERE ID_PEDIDO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al actualizar estado: " + e.getMessage(), e);
        }
    }

    private Pedido mapearPedido(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getInt("ID_PEDIDO"));
        p.setIdCliente(rs.getInt("ID_CLIENTE"));
        p.setFecha(rs.getTimestamp("FECHA").toLocalDateTime());
        p.setEstado(rs.getString("ESTADO"));
        p.setDireccionEnvio(rs.getString("DIRECCION_ENVIO"));
        p.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
        p.setImpuesto(rs.getBigDecimal("IMPUESTO"));
        p.setCostoEnvio(rs.getBigDecimal("COSTO_ENVIO"));
        p.setTotal(rs.getBigDecimal("TOTAL"));
        return p;
    }

    public int buscarIdMedioPago(String tipo) {
        String sql = "SELECT ID_MEDIO_PAGO FROM MEDIO_PAGO WHERE TIPO = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ID_MEDIO_PAGO");
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar medio de pago: " + tipo, e);
            throw new RuntimeException("Error al buscar medio de pago: " + e.getMessage(), e);
        }
        throw new RuntimeException("Medio de pago no encontrado o inactivo: " + tipo);
    }
}
