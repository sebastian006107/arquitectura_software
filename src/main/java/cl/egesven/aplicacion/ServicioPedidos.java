package cl.egesven.aplicacion;

import cl.egesven.dominio.*;
import cl.egesven.infraestructura.Conexion;
import cl.egesven.infraestructura.Logger;
import cl.egesven.infraestructura.RepositorioCarrito;
import cl.egesven.infraestructura.RepositorioPedidos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ServicioPedidos {

    private final RepositorioCarrito repositorioCarrito;
    private final RepositorioPedidos repositorioPedidos;
    private final ServicioPago servicioPago;

    public ServicioPedidos() {
        this.repositorioCarrito = new RepositorioCarrito();
        this.repositorioPedidos = new RepositorioPedidos();
        this.servicioPago = new ServicioPago();
    }

    public Pedido procesarCompra(int idCliente, String direccionEnvio, String medioPago) {
        Carrito carrito = repositorioCarrito.obtenerOCrearCarritoActivo(idCliente);
        carrito.setItems(repositorioCarrito.obtenerItems(carrito.getIdCarrito()));

        if (carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito esta vacio. Agrega productos antes de comprar.");
        }

        BigDecimal subtotal = carrito.calcularSubtotal();
        BigDecimal impuesto = carrito.calcularImpuesto();
        BigDecimal costoEnvio = carrito.calcularCostoEnvio();
        BigDecimal total = carrito.calcularTotal();

        int idMedioPago = servicioPago.obtenerIdMedioPagoPorTipo(medioPago);

        Connection conn = null;
        try {
            conn = Conexion.getInstancia().getConexion();
            conn.setAutoCommit(false);

            Pedido pedido = new Pedido();
            pedido.setIdCliente(idCliente);
            pedido.setEstado("PENDIENTE");
            pedido.setDireccionEnvio(direccionEnvio);
            pedido.setSubtotal(subtotal);
            pedido.setImpuesto(impuesto);
            pedido.setCostoEnvio(costoEnvio);
            pedido.setTotal(total);

            int idPedido = repositorioPedidos.crearPedido(conn, pedido);
            pedido.setIdPedido(idPedido);

            for (ItemCarrito item : carrito.getItems()) {
                if (item.getCantidad() > item.getProducto().getStock()) {
                    throw new RuntimeException("Stock insuficiente para: " + item.getProducto().getNombre());
                }

                DetallePedido detalle = new DetallePedido(
                        item.getIdProducto(),
                        item.getCantidad(),
                        item.getProducto().getPrecio()
                );
                detalle.setIdPedido(idPedido);
                repositorioPedidos.crearDetallePedido(conn, detalle);

                repositorioPedidos.descontarStock(conn, item.getIdProducto(), item.getCantidad());
            }

            Pago pago = servicioPago.procesarPago(idPedido, idMedioPago, total, medioPago);

            if ("RECHAZADO".equals(pago.getEstado())) {
                conn.rollback();
                throw new RuntimeException("Pago rechazado por " + medioPago + ". La compra no se realizo.");
            }

            repositorioPedidos.registrarPago(conn, pago);

            actualizarEstadoCarrito(conn, carrito.getIdCarrito());

            conn.commit();

            pedido.setEstado("PAGADO");
            pedido.setDetalles(repositorioPedidos.obtenerDetalles(idPedido));

            return pedido;

        } catch (Exception e) {
            Logger.registrar("ERROR", "Error al procesar compra para cliente: " + idCliente, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Error al procesar la compra: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    private void actualizarEstadoCarrito(Connection conn, int idCarrito) throws SQLException {
        String sql = "UPDATE CARRITO SET ESTADO = 'CONVERTIDO' WHERE ID_CARRITO = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
        }
    }

    public List<Pedido> listarPedidosCliente(int idCliente) {
        return repositorioPedidos.listarPedidosPorCliente(idCliente);
    }

    public Pedido verPedido(int idPedido) {
        return repositorioPedidos.buscarPedidoPorId(idPedido);
    }

    public List<Pedido> listarTodosPedidos() {
        return repositorioPedidos.listarTodosPedidos();
    }

    public void actualizarEstadoPedido(int idPedido, String nuevoEstado) {
        repositorioPedidos.actualizarEstado(idPedido, nuevoEstado);
    }
}
