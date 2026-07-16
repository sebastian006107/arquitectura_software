package cl.egesven.infraestructura;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/FREEPDB1";
    private static final String USER = "egesven";
    private static final String PASSWORD = "egesven123";

    private static Conexion instancia;
    private Connection conexion;

    private Conexion() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            this.conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            this.conexion.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver Oracle no encontrado. Revisa el pom.xml", e);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a Oracle: " + e.getMessage(), e);
        }
    }

    public static synchronized Conexion getInstancia() {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }

    public Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                conexion.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al reestablecer conexion: " + e.getMessage(), e);
        }
        return conexion;
    }

    public void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        }
    }
}
