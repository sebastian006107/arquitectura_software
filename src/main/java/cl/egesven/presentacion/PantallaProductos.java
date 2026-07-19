package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioProductos;
import cl.egesven.aplicacion.ServicioUsuarios;
import cl.egesven.dominio.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class PantallaProductos {

    private final ServicioProductos servicio;
    private final Scanner scanner;

    public PantallaProductos(ServicioProductos servicio, Scanner scanner) {
        this.servicio = servicio;
        this.scanner = scanner;
    }

    public void mostrarCatalogo(ServicioUsuarios.Sesion sesion) {
        System.out.println("\n========== CATALOGO DE PRODUCTOS ==========");
        List<Producto> productos = servicio.listarProductos();
        if (productos.isEmpty()) {
            System.out.println("No hay productos disponibles.");
            return;
        }
        for (Producto p : productos) {
            System.out.println(p);
        }
    }

    public void gestionarProductos() {
        while (true) {
            System.out.println("\n--- GESTION DE PRODUCTOS ---");
            System.out.println("1. Listar productos");
            System.out.println("2. Agregar producto");
            System.out.println("3. Editar producto");
            System.out.println("4. Eliminar producto");
            System.out.println("5. Volver");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine().trim();
            switch (opcion) {
                case "1" -> listarProductos();
                case "2" -> agregarProducto();
                case "3" -> editarProducto();
                case "4" -> eliminarProducto();
                case "5" -> { return; }
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private void listarProductos() {
        System.out.println("\n--- LISTADO DE PRODUCTOS ---");
        List<Producto> productos = servicio.listarProductos();
        if (productos.isEmpty()) {
            System.out.println("No hay productos registrados.");
            return;
        }
        for (Producto p : productos) {
            System.out.printf("  [%d] %s | $%,.0f | Stock: %d | %s%n",
                    p.getIdProducto(), p.getNombre(), p.getPrecio(),
                    p.getStock(), p.getCategoria() != null ? p.getCategoria() : "Sin categoria");
        }
    }

    private void agregarProducto() {
        System.out.println("\n--- AGREGAR PRODUCTO ---");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine().trim();
        System.out.print("Descripcion: ");
        String descripcion = scanner.nextLine().trim();
        System.out.print("Precio: ");
        BigDecimal precio = leerBigDecimal();
        System.out.print("Stock: ");
        int stock = leerEntero();
        System.out.print("Categoria: ");
        String categoria = scanner.nextLine().trim();

        try {
            servicio.crearProducto(nombre, descripcion, precio, stock, categoria, null);
            System.out.println("[OK] Producto creado exitosamente.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void editarProducto() {
        System.out.println("\n--- EDITAR PRODUCTO ---");
        listarProductos();
        System.out.print("\nID del producto a editar: ");
        int id = leerEntero();
        if (id <= 0) return;

        try {
            Producto p = servicio.buscarPorId(id);
            System.out.printf("Nombre actual [%s]: ", p.getNombre());
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) p.setNombre(nombre);

            System.out.printf("Descripcion actual [%s]: ", p.getDescripcion());
            String descripcion = scanner.nextLine().trim();
            if (!descripcion.isEmpty()) p.setDescripcion(descripcion);

            System.out.printf("Precio actual [%,.0f]: ", p.getPrecio());
            String precioStr = scanner.nextLine().trim();
            if (!precioStr.isEmpty()) p.setPrecio(new BigDecimal(precioStr));

            System.out.printf("Stock actual [%d]: ", p.getStock());
            String stockStr = scanner.nextLine().trim();
            if (!stockStr.isEmpty()) p.setStock(Integer.parseInt(stockStr));

            System.out.printf("Categoria actual [%s]: ", p.getCategoria());
            String categoria = scanner.nextLine().trim();
            if (!categoria.isEmpty()) p.setCategoria(categoria);

            servicio.actualizarProducto(id, p.getNombre(), p.getDescripcion(),
                    p.getPrecio(), p.getStock(), p.getCategoria(), p.getImagenUrl());
            System.out.println("[OK] Producto actualizado.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void eliminarProducto() {
        System.out.println("\n--- ELIMINAR PRODUCTO ---");
        listarProductos();
        System.out.print("\nID del producto a eliminar: ");
        int id = leerEntero();
        if (id <= 0) return;

        System.out.print("Confirmar eliminacion (S/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();
        if (!"S".equals(confirm)) {
            System.out.println("Eliminacion cancelada.");
            return;
        }

        try {
            servicio.eliminarProducto(id);
            System.out.println("[OK] Producto eliminado.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private BigDecimal leerBigDecimal() {
        try {
            return new BigDecimal(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Valor numerico invalido.");
            return BigDecimal.ZERO;
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
