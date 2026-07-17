package cl.egesven.aplicacion;

import java.util.Random;

public class ServicioMFA {

    private final Random random = new Random();
    private String codigoActual;

    public void generarYEnviar(String username) {
        codigoActual = String.valueOf(100000 + random.nextInt(900000));
        System.out.println("\n[SIMULACION MFA] Codigo enviado al dispositivo de "
                           + username + ": " + codigoActual);
    }

    public boolean validar(String codigoIngresado) {
        if (codigoActual == null || codigoIngresado == null) return false;
        boolean ok = codigoActual.equals(codigoIngresado.trim());
        codigoActual = null;
        return ok;
    }
}
