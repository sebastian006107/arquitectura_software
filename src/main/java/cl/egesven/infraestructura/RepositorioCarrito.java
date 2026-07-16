package cl.egesven.infraestructura;

import cl.egesven.dominio.Carrito;
import cl.egesven.dominio.ItemCarrito;
import cl.egesven.dominio.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositorioCarrito {

    public Carrito obtenerOCrearCarritoActivo(int idCliente) {
        String sql = "SELECT ID_CARRITO, ID_CLIENTE, FECHA_CREACION, ESTADO " +
                     "FROM CARRITO WHERE ID_CLIENTE = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Carrito c = new Carrito();
                    c.setIdCarrito(rs.getInt("ID_CARRITO"));
                    c.setIdCliente(rs.getInt("ID_CLIENTE"));
                    c.setFechaCreacion(rs.getTimestamp("FECHA_CREACION").toLocalDateTime());
                    c.setEstado(rs.getString("ESTADO"));
                    return c;
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar carrito activo para cliente: " + idCliente, e);
            throw new RuntimeException("Error al buscar carrito activo: " + e.getMessage(), e);
        }

        String insert = "INSERT INTO CARRITO (ID_CLIENTE, FECHA_CREACION, ESTADO) VALUES (?, SYSDATE, 'ACTIVO')";
        String[] cols = {"ID_CARRITO"};
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(insert, cols)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    conn.commit();
                    Carrito c = new Carrito();
                    c.setIdCarrito(rs.getInt(1));
                    c.setIdCliente(idCliente);
                    c.setEstado("ACTIVO");
                    return c;
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al crear carrito para cliente: " + idCliente, e);
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al crear carrito: " + e.getMessage(), e);
        }
        throw new RuntimeException("No se pudo crear el carrito");
    }

    public List<ItemCarrito> obtenerItems(int idCarrito) {
        List<ItemCarrito> items = new ArrayList<>();
        String sql = "SELECT i.ID_ITEM, i.ID_CARRITO, i.ID_PRODUCTO, i.CANTIDAD, " +
                     "p.ID_PRODUCTO AS PID, p.NOMBRE, p.DESCRIPCION, p.PRECIO, p.STOCK, p.CATEGORIA " +
                     "FROM ITEM_CARRITO i JOIN PRODUCTO p ON i.ID_PRODUCTO = p.ID_PRODUCTO " +
                     "WHERE i.ID_CARRITO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemCarrito item = new ItemCarrito();
                    item.setIdItem(rs.getInt("ID_ITEM"));
                    item.setIdCarrito(rs.getInt("ID_CARRITO"));
                    item.setIdProducto(rs.getInt("ID_PRODUCTO"));
                    item.setCantidad(rs.getInt("CANTIDAD"));

                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("PID"));
                    p.setNombre(rs.getString("NOMBRE"));
                    p.setDescripcion(rs.getString("DESCRIPCION"));
                    p.setPrecio(rs.getBigDecimal("PRECIO"));
                    p.setStock(rs.getInt("STOCK"));
                    p.setCategoria(rs.getString("CATEGORIA"));
                    item.setProducto(p);

                    items.add(item);
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al obtener items del carrito ID: " + idCarrito, e);
            throw new RuntimeException("Error al obtener items del carrito: " + e.getMessage(), e);
        }
        return items;
    }

    public void agregarOActualizarItem(int idCarrito, int idProducto, int cantidad) {
        String existeSql = "SELECT ID_ITEM, CANTIDAD FROM ITEM_CARRITO WHERE ID_CARRITO = ? AND ID_PRODUCTO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(existeSql)) {
            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idItem = rs.getInt("ID_ITEM");
                    int cantidadActual = rs.getInt("CANTIDAD");
                    actualizarCantidad(idItem, cantidadActual + cantidad);
                    return;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar item: " + e.getMessage(), e);
        }

        String insert = "INSERT INTO ITEM_CARRITO (ID_CARRITO, ID_PRODUCTO, CANTIDAD) VALUES (?, ?, ?)";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al agregar item: " + e.getMessage(), e);
        }
    }

    public void actualizarCantidad(int idItem, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            quitarItem(idItem);
            return;
        }
        String sql = "UPDATE ITEM_CARRITO SET CANTIDAD = ? WHERE ID_ITEM = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevaCantidad);
            ps.setInt(2, idItem);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al actualizar cantidad: " + e.getMessage(), e);
        }
    }

    public void quitarItem(int idItem) {
        String sql = "DELETE FROM ITEM_CARRITO WHERE ID_ITEM = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idItem);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al quitar item: " + e.getMessage(), e);
        }
    }

    public void vaciarCarrito(int idCarrito) {
        String sql = "DELETE FROM ITEM_CARRITO WHERE ID_CARRITO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al vaciar carrito: " + e.getMessage(), e);
        }
    }

    public void marcarComoConvertido(int idCarrito) {
        String sql = "UPDATE CARRITO SET ESTADO = 'CONVERTIDO' WHERE ID_CARRITO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al marcar carrito como convertido: " + e.getMessage(), e);
        }
    }
}
