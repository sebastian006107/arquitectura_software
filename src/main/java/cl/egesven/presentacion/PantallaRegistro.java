package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioUsuarios;

import java.util.Scanner;

public class PantallaRegistro {

    private final ServicioUsuarios servicio;
    private final Scanner scanner;

    public PantallaRegistro(ServicioUsuarios servicio, Scanner scanner) {
        this.servicio = servicio;
        this.scanner = scanner;
    }

    public void mostrar() {
        System.out.println("\n========== REGISTRO DE CLIENTE ==========");
        System.out.print("Nombre completo: ");
        String nombre = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Direccion de envio: ");
        String direccion = scanner.nextLine().trim();

        try {
            servicio.registrarCliente(username, password, nombre, email, direccion);
            System.out.println("\n[OK] Cliente registrado exitosamente. Ya puedes iniciar sesion.");
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }
}
