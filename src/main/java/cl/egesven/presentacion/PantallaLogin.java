package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioUsuarios;

import java.util.Scanner;

public class PantallaLogin {

    private final ServicioUsuarios servicio;
    private final Scanner scanner;

    public PantallaLogin(ServicioUsuarios servicio, Scanner scanner) {
        this.servicio = servicio;
        this.scanner = scanner;
    }

    public ServicioUsuarios.Sesion mostrar() {
        System.out.println("\n========== INICIAR SESION ==========");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            ServicioUsuarios.Sesion sesion = servicio.login(username, password);
            System.out.println("\n[OK] Bienvenido/a, " + username + " (" + sesion.getTipoUsuario() + ")");
            return sesion;
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
            return null;
        }
    }
}
