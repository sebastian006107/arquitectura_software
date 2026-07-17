package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioCarrito;
import cl.egesven.aplicacion.ServicioMFA;
import cl.egesven.aplicacion.ServicioPedidos;
import cl.egesven.aplicacion.ServicioProductos;
import cl.egesven.aplicacion.ServicioUsuarios;
import cl.egesven.dominio.Log;
import cl.egesven.dominio.Pedido;
import cl.egesven.infraestructura.Conexion;
import cl.egesven.infraestructura.Logger;

import java.util.List;
import java.util.Scanner;

public class Menu {

    private final ServicioUsuarios servicioUsuarios;
    private final ServicioMFA servicioMFA;
    private final ServicioProductos servicioProductos;
    private final ServicioCarrito servicioCarrito;
    private final ServicioPedidos servicioPedidos;
    private final PantallaProductos pantallaProductos;
    private final PantallaCarrito pantallaCarrito;
    private final PantallaPedidos pantallaPedidos;
    private final Scanner scanner;
    private ServicioUsuarios.Sesion sesion;

    public Menu() {
        this.servicioUsuarios = new ServicioUsuarios();
        this.servicioMFA = new ServicioMFA();
        this.servicioProductos = new ServicioProductos();
        this.servicioCarrito = new ServicioCarrito();
        this.servicioPedidos = new ServicioPedidos();
        this.scanner = new Scanner(System.in);
        this.pantallaProductos = new PantallaProductos(servicioProductos, scanner);
        this.pantallaCarrito = new PantallaCarrito(servicioCarrito, servicioProductos, scanner);
        this.pantallaPedidos = new PantallaPedidos(servicioPedidos, scanner);
    }

    public void iniciar() {
        actualizarHashesDemo();
        System.out.println("========== eGESVEN — Plataforma de Comercio Electronico ==========");

        while (true) {
            if (sesion == null) {
                mostrarMenuPrincipal();
            } else if (sesion.esCliente()) {
                mostrarMenuCliente();
            } else if (sesion.esAdmin()) {
                mostrarMenuAdmin();
            }
        }
    }

    private void mostrarMenuPrincipal() {
        System.out.println("\n--- MENU PRINCIPAL ---");
        System.out.println("1. Iniciar sesion");
        System.out.println("2. Registrarse");
        System.out.println("3. Salir");
        System.out.print("Opcion: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1" -> {
                PantallaLogin login = new PantallaLogin(servicioUsuarios, servicioMFA, scanner);
                sesion = login.mostrar();
            }
            case "2" -> {
                PantallaRegistro registro = new PantallaRegistro(servicioUsuarios, scanner);
                registro.mostrar();
            }
            case "3" -> {
                System.out.println("\nHasta luego.");
                Conexion.getInstancia().cerrar();
                System.exit(0);
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    private void mostrarMenuCliente() {
        System.out.println("\n--- MENU CLIENTE | " + sesion.getCliente().getNombre() + " ---");
        System.out.println("1. Ver catalogo de productos");
        System.out.println("2. Ver carrito");
        System.out.println("3. Comprar");
        System.out.println("4. Mis pedidos");
        System.out.println("5. Cerrar sesion");
        System.out.print("Opcion: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1" -> {
                        pantallaProductos.mostrarCatalogo(sesion);
                        System.out.print("\nAgregar producto al carrito? (S/N): ");
                        if ("S".equalsIgnoreCase(scanner.nextLine().trim())) {
                            pantallaCarrito.agregarProducto(sesion.getCliente().getIdCliente());
                        }
                    }
                    case "2" -> pantallaCarrito.verCarrito(sesion.getCliente().getIdCliente());
            case "3" -> realizarCompra();
            case "4" -> pantallaPedidos.verPedidosCliente(sesion.getCliente().getIdCliente());
            case "5" -> {
                sesion = null;
                System.out.println("Sesion cerrada.");
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    private void mostrarMenuAdmin() {
        System.out.println("\n--- MENU ADMINISTRADOR | " + sesion.getAdministrador().getNombre() + " ---");
        System.out.println("1. Gestionar productos");
        System.out.println("2. Gestionar pedidos");
        System.out.println("3. Revisar logs");
        System.out.println("4. Cerrar sesion");
        System.out.print("Opcion: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1" -> pantallaProductos.gestionarProductos();
            case "2" -> pantallaPedidos.gestionarPedidos();
            case "3" -> revisarLogs();
            case "4" -> {
                sesion = null;
                System.out.println("Sesion cerrada.");
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    private void realizarCompra() {
        int idCliente = sesion.getCliente().getIdCliente();
        pantallaCarrito.verCarrito(idCliente);

        System.out.println("\n--- CONFIRMAR COMPRA ---");
        System.out.println("Medio de pago:");
        System.out.println("1. Transbank (debito/credito)");
        System.out.println("2. PayPal");
        System.out.println("3. Cancelar");
        System.out.print("Opcion: ");

        String opcion = scanner.nextLine().trim();
        String medioPago;
        switch (opcion) {
            case "1" -> medioPago = "TRANSBANK";
            case "2" -> medioPago = "PAYPAL";
            default -> {
                System.out.println("Compra cancelada.");
                return;
            }
        }

        System.out.print("Confirmar compra? (S/N): ");
        if (!"S".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Compra cancelada.");
            return;
        }

        String direccion = sesion.getCliente().getDireccionPorDefecto();
        if (direccion == null || direccion.isBlank()) {
            System.out.print("Ingresa direccion de envio: ");
            direccion = scanner.nextLine().trim();
        }

        try {
            Pedido pedido = servicioPedidos.procesarCompra(idCliente, direccion, medioPago);
            System.out.println("\n[OK] Compra realizada con exito!");
            System.out.println("Pedido #" + pedido.getIdPedido() + " | Total: $" + pedido.getTotal());
            System.out.println("Estado: " + pedido.getEstado());
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private void revisarLogs() {
        System.out.println("\n========== LOGS DEL SISTEMA (ultimos 20) ==========");
        List<Log> logs = Logger.leerUltimos(20);
        if (logs.isEmpty()) {
            System.out.println("No hay registros en el log.");
            return;
        }
        for (Log log : logs) {
            System.out.printf("  [%s] %s | %s%n",
                    log.getFechaHora() != null ? log.getFechaHora().toString().substring(0, 19) : "N/A",
                    log.getTipo(), log.getMensaje());
        }
    }

    private void actualizarHashesDemo() {
        try {
            servicioUsuarios.actualizarPasswordDemo("admin", "admin123");
            servicioUsuarios.actualizarPasswordDemo("jperez", "cliente123");
        } catch (Exception e) {
            System.err.println("[AVISO] No se pudieron actualizar los hashes demo: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Menu().iniciar();
    }
}
