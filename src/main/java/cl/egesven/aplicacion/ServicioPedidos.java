package cl.egesven.aplicacion;

import cl.egesven.dominio.*;
import cl.egesven.infraestructura.Conexion;
import cl.egesven.infraestructura.Logger;
import cl.egesven.infraestructura.RepositorioCarrito;
import cl.egesven.infraestructura.RepositorioClientes;
import cl.egesven.infraestructura.RepositorioPedidos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ServicioPedidos {

    private final RepositorioCarrito repositorioCarrito;
    private final RepositorioPedidos repositorioPedidos;
    private final RepositorioClientes repositorioClientes;
    private final ServicioPago servicioPago;

    public ServicioPedidos() {
        this.repositorioCarrito = new RepositorioCarrito();
        this.repositorioPedidos = new RepositorioPedidos();
        this.repositorioClientes = new RepositorioClientes();
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

            // El pago quedo APROBADO: el pedido pasa a PAGADO dentro de la misma
            // transaccion, para que la BD no quede en PENDIENTE.
            repositorioPedidos.actualizarEstado(conn, idPedido, "PAGADO");
            pedido.setEstado("PAGADO");

            actualizarEstadoCarrito(conn, carrito.getIdCarrito());

            conn.commit();

            pedido.setDetalles(repositorioPedidos.obtenerDetalles(idPedido));

            return pedido;

        } catch (Exception e) {
            // El rollback va ANTES de registrar el log: el Logger comparte la misma
            // conexion y hace commit, lo que confirmaria la transaccion fallida.
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            Logger.registrar("ERROR", "Error al procesar compra para cliente: " + idCliente, e);
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

    /**
     * RF010: arma el recibo de un pedido ya realizado.
     * Los montos salen del pedido persistido, no se recalculan.
     */
    public Recibo generarRecibo(int idPedido) {
        Pedido pedido = repositorioPedidos.buscarPedidoPorId(idPedido);
        if (pedido == null) {
            throw new RuntimeException("El pedido #" + idPedido + " no existe");
        }
        Cliente cliente = repositorioClientes.buscarClientePorId(pedido.getIdCliente());
        Pago pago = repositorioPedidos.buscarPagoPorPedido(idPedido);
        return new Recibo(pedido, cliente, pago);
    }

    public List<Pedido> listarTodosPedidos() {
        return repositorioPedidos.listarTodosPedidos();
    }

    public void actualizarEstadoPedido(int idPedido, String nuevoEstado) {
        repositorioPedidos.actualizarEstado(idPedido, nuevoEstado);
    }
}
