package cl.egesven.infraestructura;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class Logger {

    public static void registrar(String tipo, String mensaje, String traza) {
        String sql = "INSERT INTO LOG_SISTEMA (FECHA_HORA, TIPO, MENSAJE, TRAZA) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = Conexion.getInstancia().getConexion();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setString(2, tipo);
                ps.setString(3, mensaje.length() > 500 ? mensaje.substring(0, 497) + "..." : mensaje);
                if (traza != null && !traza.isEmpty()) {
                    ps.setString(4, traza);
                } else {
                    ps.setNull(4, java.sql.Types.CLOB);
                }
                ps.executeUpdate();
                conn.commit();
            }
        } catch (Exception e) {
            System.err.println("[LOGGER] No se pudo registrar en LOG_SISTEMA: " + e.getMessage());
        }
    }

    public static void registrar(String tipo, String mensaje, Exception ex) {
        String traza = null;
        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            traza = sw.toString();
        }
        registrar(tipo, mensaje, traza);
    }
}
