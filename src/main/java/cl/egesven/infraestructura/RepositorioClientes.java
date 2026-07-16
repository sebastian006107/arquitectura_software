package cl.egesven.infraestructura;

import cl.egesven.dominio.Administrador;
import cl.egesven.dominio.Cliente;
import cl.egesven.dominio.UsuarioSistema;

import java.sql.*;

public class RepositorioClientes {

    public UsuarioSistema buscarPorUsername(String username) {
        String sql = "SELECT ID_USUARIO, USERNAME, PASSWORD_HASH, TIPO_USUARIO, ACTIVO " +
                     "FROM USUARIO_SISTEMA WHERE USERNAME = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UsuarioSistema u = new UsuarioSistema();
                    u.setIdUsuario(rs.getInt("ID_USUARIO"));
                    u.setUsername(rs.getString("USERNAME"));
                    u.setPasswordHash(rs.getString("PASSWORD_HASH"));
                    u.setTipoUsuario(rs.getString("TIPO_USUARIO"));
                    u.setActivo(rs.getString("ACTIVO"));
                    return u;
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar usuario: " + username, e);
            throw new RuntimeException("Error al buscar usuario: " + e.getMessage(), e);
        }
        return null;
    }

    public int registrarUsuario(UsuarioSistema usuario) {
        String sql = "INSERT INTO USUARIO_SISTEMA (USERNAME, PASSWORD_HASH, TIPO_USUARIO, ACTIVO) " +
                     "VALUES (?, ?, ?, 'S')";
        String[] columnasId = {"ID_USUARIO"};
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, columnasId)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getTipoUsuario());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    conn.commit();
                    return rs.getInt(1);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al registrar usuario: " + usuario.getUsername(), e);
            try {
                Conexion.getInstancia().getConexion().rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Error en rollback: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
        throw new RuntimeException("No se pudo obtener el ID generado para el usuario");
    }

    public int registrarCliente(Cliente cliente) {
        String sql = "INSERT INTO CLIENTE (ID_USUARIO, NOMBRE, EMAIL, DIRECCION_POR_DEFECTO) " +
                     "VALUES (?, ?, ?, ?)";
        String[] columnasId = {"ID_CLIENTE"};
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, columnasId)) {
            ps.setInt(1, cliente.getIdUsuario());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getDireccionPorDefecto());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    conn.commit();
                    return rs.getInt(1);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al registrar cliente: " + cliente.getEmail(), e);
            try {
                Conexion.getInstancia().getConexion().rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Error en rollback: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Error al registrar cliente: " + e.getMessage(), e);
        }
        throw new RuntimeException("No se pudo obtener el ID generado para el cliente");
    }

    public Cliente buscarClientePorIdUsuario(int idUsuario) {
        String sql = "SELECT ID_CLIENTE, ID_USUARIO, NOMBRE, EMAIL, DIRECCION_POR_DEFECTO " +
                     "FROM CLIENTE WHERE ID_USUARIO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("ID_CLIENTE"));
                    c.setIdUsuario(rs.getInt("ID_USUARIO"));
                    c.setNombre(rs.getString("NOMBRE"));
                    c.setEmail(rs.getString("EMAIL"));
                    c.setDireccionPorDefecto(rs.getString("DIRECCION_POR_DEFECTO"));
                    return c;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente: " + e.getMessage(), e);
        }
        return null;
    }

    public Administrador buscarAdminPorIdUsuario(int idUsuario) {
        String sql = "SELECT ID_ADMIN, ID_USUARIO, NOMBRE, EMAIL, ROL " +
                     "FROM ADMINISTRADOR WHERE ID_USUARIO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Administrador a = new Administrador();
                    a.setIdAdmin(rs.getInt("ID_ADMIN"));
                    a.setIdUsuario(rs.getInt("ID_USUARIO"));
                    a.setNombre(rs.getString("NOMBRE"));
                    a.setEmail(rs.getString("EMAIL"));
                    a.setRol(rs.getString("ROL"));
                    return a;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar admin: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean existeUsername(String username) {
        String sql = "SELECT COUNT(*) FROM USUARIO_SISTEMA WHERE USERNAME = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar username: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean existeEmail(String email) {
        String sql = "SELECT COUNT(*) FROM CLIENTE WHERE EMAIL = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar email: " + e.getMessage(), e);
        }
        return false;
    }
}
