package cl.egesven.infraestructura;

import cl.egesven.dominio.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

    public static List<Log> leerUltimos(int n) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT ID_LOG, FECHA_HORA, TIPO, MENSAJE, TRAZA FROM LOG_SISTEMA ORDER BY FECHA_HORA DESC FETCH FIRST ? ROWS ONLY";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Log log = new Log();
                    log.setIdLog(rs.getInt("ID_LOG"));
                    Timestamp ts = rs.getTimestamp("FECHA_HORA");
                    if (ts != null) {
                        log.setFechaHora(ts.toLocalDateTime());
                    }
                    log.setTipo(rs.getString("TIPO"));
                    log.setMensaje(rs.getString("MENSAJE"));
                    log.setTraza(rs.getString("TRAZA"));
                    logs.add(log);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al leer logs: " + e.getMessage(), e);
        }
        return logs;
    }
}
