package cl.egesven.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Carrito {
    private int idCarrito;
    private int idCliente;
    private LocalDateTime fechaCreacion;
    private String estado;
    private List<ItemCarrito> items;

    public Carrito() {
        this.items = new ArrayList<>();
    }

    public BigDecimal calcularSubtotal() {
        return items.stream()
                .map(ItemCarrito::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularImpuesto() {
        return calcularSubtotal()
                .multiply(new BigDecimal("0.19"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularCostoEnvio() {
        BigDecimal subtotal = calcularSubtotal();
        return subtotal.compareTo(new BigDecimal("50000")) > 0
                ? BigDecimal.ZERO
                : new BigDecimal("3990");
    }

    public BigDecimal calcularTotal() {
        return calcularSubtotal()
                .add(calcularImpuesto())
                .add(calcularCostoEnvio())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String obtenerDesglose() {
        return String.format(
                "========== DESGLOSE DE COMPRA ==========%n" +
                "  Subtotal:    $%,.0f%n" +
                "  IVA (19%%):   $%,.0f%n" +
                "  Costo envío:  $%,.0f%s%n" +
                "  -----------------------------%n" +
                "  TOTAL:        $%,.0f%n" +
                "========================================%n",
                calcularSubtotal(),
                calcularImpuesto(),
                calcularCostoEnvio(),
                calcularCostoEnvio().compareTo(BigDecimal.ZERO) == 0 ? " (envío gratis)" : "",
                calcularTotal());
    }

    public int getIdCarrito() { return idCarrito; }
    public void setIdCarrito(int idCarrito) { this.idCarrito = idCarrito; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<ItemCarrito> getItems() { return items; }
    public void setItems(List<ItemCarrito> items) { this.items = items; }
}
