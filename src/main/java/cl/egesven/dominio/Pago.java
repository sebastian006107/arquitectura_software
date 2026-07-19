package cl.egesven.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {
    private int idPago;
    private int idPedido;
    private int idMedioPago;
    private String tokenTransaccion;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private String estado;
    /** Tipo del medio de pago (TRANSBANK/PAYPAL) resuelto para mostrar en el recibo (RF010). */
    private String tipoMedioPago;

    public Pago() {}

    public Pago(int idPedido, int idMedioPago, BigDecimal monto, String tokenTransaccion) {
        this.idPedido = idPedido;
        this.idMedioPago = idMedioPago;
        this.monto = monto;
        this.tokenTransaccion = tokenTransaccion;
    }

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }

    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }

    public int getIdMedioPago() { return idMedioPago; }
    public void setIdMedioPago(int idMedioPago) { this.idMedioPago = idMedioPago; }

    public String getTokenTransaccion() { return tokenTransaccion; }
    public void setTokenTransaccion(String tokenTransaccion) { this.tokenTransaccion = tokenTransaccion; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTipoMedioPago() { return tipoMedioPago; }
    public void setTipoMedioPago(String tipoMedioPago) { this.tipoMedioPago = tipoMedioPago; }
}
