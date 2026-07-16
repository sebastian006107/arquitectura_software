package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioCarrito;
import cl.egesven.aplicacion.ServicioProductos;
import cl.egesven.dominio.Carrito;
import cl.egesven.dominio.ItemCarrito;
import cl.egesven.dominio.Producto;

import java.util.List;
import java.util.Scanner;

public class PantallaCarrito {

    private final ServicioCarrito servicioCarrito;
    private final ServicioProductos servicioProductos;
    private final Scanner scanner;

    public PantallaCarrito(ServicioCarrito servicioCarrito, ServicioProductos servicioProductos, Scanner scanner) {
        this.servicioCarrito = servicioCarrito;
        this.servicioProductos = servicioProductos;
        this.scanner = scanner;
    }

    public void verCarrito(int idCliente) {
        Carrito carrito = servicioCarrito.obtenerCarrito(idCliente);

        System.out.println("\n========== TU CARRITO ==========");
        if (carrito.getItems().isEmpty()) {
            System.out.println("Tu carrito esta vacio.");
            return;
        }

        for (ItemCarrito item : carrito.getItems()) {
            System.out.printf("  [%d] %s%n", item.getIdItem(), item);
        }

        System.out.println(carrito.obtenerDesglose());

        System.out.println("1. Modificar cantidad de un item");
        System.out.println("2. Eliminar un item");
        System.out.println("3. Volver");
        System.out.print("Opcion: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1" -> modificarCantidad(carrito);
            case "2" -> eliminarItem();
            default -> {}
        }
    }

    public void agregarProducto(int idCliente) {
        System.out.println("\n--- AGREGAR AL CARRITO ---");
        List<Producto> productos = servicioProductos.listarProductos();
        if (productos.isEmpty()) {
            System.out.println("No hay productos disponibles.");
            return;
        }
        for (Producto p : productos) {
            System.out.printf("  [%d] %s — $%,.0f (Stock: %d)%n",
                    p.getIdProducto(), p.getNombre(), p.getPrecio(), p.getStock());
        }

        System.out.print("\nID del producto: ");
        int idProducto = leerEntero();
        if (idProducto <= 0) return;

        Producto producto = servicioProductos.buscarPorId(idProducto);
        System.out.printf("Producto: %s | Stock disponible: %d%n", producto.getNombre(), producto.getStock());
        System.out.print("Cantidad: ");
        int cantidad = leerEntero();
        if (cantidad <= 0) return;

        if (cantidad > producto.getStock()) {
            System.out.println("[ERROR] No hay suficiente stock.");
            return;
        }

        try {
            servicioCarrito.agregarProducto(idCliente, idProducto, cantidad);
            System.out.println("[OK] Producto agregado al carrito.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void modificarCantidad(Carrito carrito) {
        System.out.print("ID del item a modificar: ");
        int idItem = leerEntero();
        if (idItem <= 0) return;

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getIdItem() == idItem)
                .findFirst().orElse(null);
        if (item == null) {
            System.out.println("[ERROR] Item no encontrado en el carrito.");
            return;
        }

        System.out.printf("Cantidad actual: %d. Nueva cantidad (0 para eliminar): ", item.getCantidad());
        int nuevaCantidad = leerEntero();
        if (nuevaCantidad < 0) return;

        try {
            servicioCarrito.actualizarCantidad(idItem, nuevaCantidad);
            System.out.println("[OK] Cantidad actualizada.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void eliminarItem() {
        System.out.print("ID del item a eliminar: ");
        int idItem = leerEntero();
        if (idItem <= 0) return;

        try {
            servicioCarrito.quitarProducto(idItem);
            System.out.println("[OK] Item eliminado del carrito.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private int leerEntero() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Valor numerico invalido.");
            return -1;
        }
    }
}
