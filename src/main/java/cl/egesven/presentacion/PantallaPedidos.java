package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioPedidos;
import cl.egesven.dominio.DetallePedido;
import cl.egesven.dominio.Pedido;

import java.util.List;
import java.util.Scanner;

public class PantallaPedidos {

    private final ServicioPedidos servicioPedidos;
    private final Scanner scanner;

    private static final String[] ESTADOS_VALIDOS = {
        "PENDIENTE", "PAGADO", "EN_PREPARACION", "DESPACHADO", "ENTREGADO", "ANULADO"
    };

    public PantallaPedidos(ServicioPedidos servicioPedidos, Scanner scanner) {
        this.servicioPedidos = servicioPedidos;
        this.scanner = scanner;
    }

    public void verPedidosCliente(int idCliente) {
        System.out.println("\n========== MIS PEDIDOS ==========");
        List<Pedido> pedidos = servicioPedidos.listarPedidosCliente(idCliente);

        if (pedidos.isEmpty()) {
            System.out.println("No tienes pedidos registrados.");
            return;
        }

        for (Pedido p : pedidos) {
            System.out.printf("  [#%d] %s | Estado: %s | Total: $%,.0f%n",
                    p.getIdPedido(),
                    p.getFecha() != null ? p.getFecha().toLocalDate() : "N/A",
                    p.getEstado(), p.getTotal());
        }

        System.out.print("\nVer detalle de un pedido? (ID o 0 para volver): ");
        int idPedido = leerEntero();
        if (idPedido > 0) {
            verDetallePedido(idPedido);
        }
    }

    public void gestionarPedidos() {
        while (true) {
            System.out.println("\n--- GESTION DE PEDIDOS ---");
            List<Pedido> pedidos = servicioPedidos.listarTodosPedidos();

            if (pedidos.isEmpty()) {
                System.out.println("No hay pedidos registrados.");
                return;
            }

            for (Pedido p : pedidos) {
                System.out.printf("  [#%d] Cliente: %d | %s | Estado: %s | Total: $%,.0f%n",
                        p.getIdPedido(), p.getIdCliente(),
                        p.getFecha() != null ? p.getFecha().toLocalDate() : "N/A",
                        p.getEstado(), p.getTotal());
            }

            System.out.println("\n1. Ver detalle de un pedido");
            System.out.println("2. Cambiar estado de un pedido");
            System.out.println("3. Volver");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine().trim();
            switch (opcion) {
                case "1" -> {
                    System.out.print("ID del pedido: ");
                    int id = leerEntero();
                    if (id > 0) verDetallePedido(id);
                }
                case "2" -> cambiarEstadoPedido();
                case "3" -> { return; }
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private void verDetallePedido(int idPedido) {
        Pedido pedido = servicioPedidos.verPedido(idPedido);
        if (pedido == null) {
            System.out.println("[ERROR] Pedido no encontrado.");
            return;
        }

        System.out.println("\n========== DETALLE PEDIDO #" + idPedido + " ==========");
        System.out.printf("  Fecha: %s%n", pedido.getFecha() != null ? pedido.getFecha().toLocalDate() : "N/A");
        System.out.printf("  Estado: %s%n", pedido.getEstado());
        System.out.printf("  Direccion envio: %s%n", pedido.getDireccionEnvio());
        System.out.println("  --- Items ---");

        for (DetallePedido d : pedido.getDetalles()) {
            System.out.printf("    %dx Producto #%d | Precio unitario: $%,.0f | Subtotal: $%,.0f%n",
                    d.getCantidad(), d.getIdProducto(), d.getPrecioUnitario(), d.getSubtotal());
        }

        System.out.println("  --- Totales ---");
        System.out.printf("  Subtotal:   $%,.0f%n", pedido.getSubtotal());
        System.out.printf("  IVA (19%%):  $%,.0f%n", pedido.getImpuesto());
        System.out.printf("  Envio:      $%,.0f%n", pedido.getCostoEnvio());
        System.out.printf("  TOTAL:      $%,.0f%n", pedido.getTotal());
    }

    private void cambiarEstadoPedido() {
        System.out.print("ID del pedido: ");
        int idPedido = leerEntero();
        if (idPedido <= 0) return;

        System.out.println("Estados disponibles:");
        for (int i = 0; i < ESTADOS_VALIDOS.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, ESTADOS_VALIDOS[i]);
        }
        System.out.print("Nuevo estado: ");
        int idx = leerEntero();
        if (idx < 1 || idx > ESTADOS_VALIDOS.length) {
            System.out.println("[ERROR] Estado no valido.");
            return;
        }

        try {
            servicioPedidos.actualizarEstadoPedido(idPedido, ESTADOS_VALIDOS[idx - 1]);
            System.out.println("[OK] Estado actualizado.");
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
