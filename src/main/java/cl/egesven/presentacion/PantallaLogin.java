package cl.egesven.presentacion;

import cl.egesven.aplicacion.ServicioMFA;
import cl.egesven.aplicacion.ServicioUsuarios;

import java.util.Scanner;

public class PantallaLogin {

    private final ServicioUsuarios servicio;
    private final ServicioMFA mfa;
    private final Scanner scanner;

    public PantallaLogin(ServicioUsuarios servicio, ServicioMFA mfa, Scanner scanner) {
        this.servicio = servicio;
        this.mfa = mfa;
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

            mfa.generarYEnviar(username);
            System.out.print("Codigo de verificacion: ");
            String codigo = scanner.nextLine().trim();

            if (!mfa.validar(codigo)) {
                System.out.println("\n[ERROR] Codigo de verificacion incorrecto. Acceso denegado.");
                return null;
            }

            System.out.println("\n[OK] Bienvenido/a, " + username + " (" + sesion.getTipoUsuario() + ")");
            return sesion;
        } catch (RuntimeException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
            return null;
        }
    }
}
