package cl.egesven.dominio;

import java.math.BigDecimal;

public class ItemCarrito {
    private int idItem;
    private int idCarrito;
    private int idProducto;
    private int cantidad;
    private Producto producto;

    public ItemCarrito() {}

    public ItemCarrito(int idCarrito, int idProducto, int cantidad) {
        this.idCarrito = idCarrito;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public BigDecimal calcularSubtotal() {
        if (producto == null) return BigDecimal.ZERO;
        return producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }

    public int getIdItem() { return idItem; }
    public void setIdItem(int idItem) { this.idItem = idItem; }

    public int getIdCarrito() { return idCarrito; }
    public void setIdCarrito(int idCarrito) { this.idCarrito = idCarrito; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    @Override
    public String toString() {
        String nombre = producto != null ? producto.getNombre() : "ID:" + idProducto;
        return String.format("%dx %s — $%,.0f c/u — Subtotal: $%,.0f",
                cantidad, nombre,
                producto != null ? producto.getPrecio() : BigDecimal.ZERO,
                calcularSubtotal());
    }
}
