package cl.egesven.aplicacion;

import cl.egesven.dominio.Producto;
import cl.egesven.infraestructura.RepositorioProductos;

import java.math.BigDecimal;
import java.util.List;

public class ServicioProductos {

    private final RepositorioProductos repositorio;

    public ServicioProductos() {
        this.repositorio = new RepositorioProductos();
    }

    public List<Producto> listarProductos() {
        return repositorio.listarTodos();
    }

    public Producto buscarPorId(int idProducto) {
        Producto p = repositorio.buscarPorId(idProducto);
        if (p == null) {
            throw new RuntimeException("Producto no encontrado");
        }
        return p;
    }

    public void crearProducto(String nombre, String descripcion,
                               BigDecimal precio, int stock, String categoria, String imagenUrl) {
        if (nombre == null || nombre.isBlank()) {
            throw new RuntimeException("El nombre es obligatorio");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El precio no puede ser negativo");
        }
        if (stock < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        Producto p = new Producto(nombre, descripcion, precio, stock, categoria);
        p.setImagenUrl(imagenUrl);
        int id = repositorio.crear(p);
        p.setIdProducto(id);
    }

    public void actualizarProducto(int idProducto, String nombre, String descripcion,
                                    BigDecimal precio, int stock, String categoria, String imagenUrl) {
        Producto p = repositorio.buscarPorId(idProducto);
        if (p == null) {
            throw new RuntimeException("Producto no encontrado");
        }

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setCategoria(categoria);
        p.setImagenUrl(imagenUrl);

        repositorio.actualizar(p);
    }

    public void eliminarProducto(int idProducto) {
        repositorio.eliminar(idProducto);
    }
}
