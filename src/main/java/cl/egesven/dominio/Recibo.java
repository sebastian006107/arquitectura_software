package cl.egesven.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF010: comprobante de una compra ya realizada.
 *
 * No tiene tabla propia: se arma a partir de PEDIDO, DETALLE_PEDIDO, CLIENTE y PAGO.
 * Los montos se leen del pedido persistido (no se recalculan), de modo que el recibo
 * refleje exactamente lo que quedo en la BD, con el precio unitario ya congelado.
 */
public class Recibo {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Pedido pedido;
    private final Cliente cliente;
    private final Pago pago;

    public Recibo(Pedido pedido, Cliente cliente, Pago pago) {
        this.pedido = pedido;
        this.cliente = cliente;
        this.pago = pago;
    }

    public String getNumero() {
        return String.format("R-%06d", pedido.getIdPedido());
    }

    public int getIdPedido() { return pedido.getIdPedido(); }
    public int getIdCliente() { return pedido.getIdCliente(); }
    public String getEstadoPedido() { return pedido.getEstado(); }
    public String getDireccionEnvio() { return pedido.getDireccionEnvio(); }
    public List<DetallePedido> getDetalles() { return pedido.getDetalles(); }

    public BigDecimal getSubtotal() { return pedido.getSubtotal(); }
    public BigDecimal getImpuesto() { return pedido.getImpuesto(); }
    public BigDecimal getCostoEnvio() { return pedido.getCostoEnvio(); }
    public BigDecimal getTotal() { return pedido.getTotal(); }

    public String getNombreCliente() { return cliente != null ? cliente.getNombre() : "—"; }
    public String getEmailCliente() { return cliente != null ? cliente.getEmail() : "—"; }

    public String getMedioPago() { return pago != null ? pago.getTipoMedioPago() : "—"; }
    public String getTokenTransaccion() { return pago != null ? pago.getTokenTransaccion() : "—"; }
    public String getEstadoPago() { return pago != null ? pago.getEstado() : "—"; }

    public String getFechaEmision() {
        LocalDateTime fecha = pedido.getFecha();
        return fecha != null ? fecha.format(FORMATO_FECHA) : "—";
    }

}
