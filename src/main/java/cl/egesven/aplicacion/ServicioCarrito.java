package cl.egesven.aplicacion;

import cl.egesven.dominio.Carrito;
import cl.egesven.infraestructura.RepositorioCarrito;

public class ServicioCarrito {

    private final RepositorioCarrito repositorio;

    public ServicioCarrito() {
        this.repositorio = new RepositorioCarrito();
    }

    public Carrito obtenerCarrito(int idCliente) {
        Carrito carrito = repositorio.obtenerOCrearCarritoActivo(idCliente);
        carrito.setItems(repositorio.obtenerItems(carrito.getIdCarrito()));
        return carrito;
    }

    public void agregarProducto(int idCliente, int idProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }
        Carrito carrito = repositorio.obtenerOCrearCarritoActivo(idCliente);
        repositorio.agregarOActualizarItem(carrito.getIdCarrito(), idProducto, cantidad);
    }

    public void quitarProducto(int idItem) {
        repositorio.quitarItem(idItem);
    }

    public void actualizarCantidad(int idItem, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            repositorio.quitarItem(idItem);
        } else {
            repositorio.actualizarCantidad(idItem, nuevaCantidad);
        }
    }
}
