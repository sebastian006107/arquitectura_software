package cl.egesven.infraestructura;

import cl.egesven.dominio.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositorioProductos {

    public List<Producto> listarTodos() {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT ID_PRODUCTO, NOMBRE, DESCRIPCION, PRECIO, STOCK, CATEGORIA, IMAGEN_URL " +
                     "FROM PRODUCTO ORDER BY CATEGORIA, NOMBRE";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al listar productos", e);
            throw new RuntimeException("Error al listar productos: " + e.getMessage(), e);
        }
        return productos;
    }

    /**
     * RF005: busca por texto libre (nombre o descripcion) y/o por categoria.
     * Ambos filtros son opcionales; si los dos vienen vacios equivale a listarTodos().
     */
    public List<Producto> buscar(String texto, String categoria) {
        StringBuilder sql = new StringBuilder(
                "SELECT ID_PRODUCTO, NOMBRE, DESCRIPCION, PRECIO, STOCK, CATEGORIA, IMAGEN_URL " +
                "FROM PRODUCTO WHERE 1 = 1");
        List<String> parametros = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (UPPER(NOMBRE) LIKE ? OR UPPER(DESCRIPCION) LIKE ?)");
            String patron = "%" + texto.trim().toUpperCase() + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        if (categoria != null && !categoria.isBlank()) {
            sql.append(" AND UPPER(CATEGORIA) = ?");
            parametros.add(categoria.trim().toUpperCase());
        }
        sql.append(" ORDER BY CATEGORIA, NOMBRE");

        List<Producto> productos = new ArrayList<>();
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar productos (texto=" + texto
                    + ", categoria=" + categoria + ")", e);
            throw new RuntimeException("Error al buscar productos: " + e.getMessage(), e);
        }
        return productos;
    }

    /** Alimenta el filtro por categoria de la pantalla de catalogo (RF005). */
    public List<String> listarCategorias() {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT DISTINCT CATEGORIA FROM PRODUCTO " +
                     "WHERE CATEGORIA IS NOT NULL ORDER BY CATEGORIA";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.add(rs.getString("CATEGORIA"));
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al listar categorias", e);
            throw new RuntimeException("Error al listar categorias: " + e.getMessage(), e);
        }
        return categorias;
    }

    public Producto buscarPorId(int idProducto) {
        String sql = "SELECT ID_PRODUCTO, NOMBRE, DESCRIPCION, PRECIO, STOCK, CATEGORIA, IMAGEN_URL " +
                     "FROM PRODUCTO WHERE ID_PRODUCTO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al buscar producto ID: " + idProducto, e);
            throw new RuntimeException("Error al buscar producto: " + e.getMessage(), e);
        }
        return null;
    }

    public int crear(Producto producto) {
        String sql = "INSERT INTO PRODUCTO (NOMBRE, DESCRIPCION, PRECIO, STOCK, CATEGORIA, IMAGEN_URL) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        String[] columnasId = {"ID_PRODUCTO"};
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, columnasId)) {
            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setBigDecimal(3, producto.getPrecio());
            ps.setInt(4, producto.getStock());
            ps.setString(5, producto.getCategoria());
            ps.setString(6, producto.getImagenUrl());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    conn.commit();
                    return rs.getInt(1);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al crear producto: " + producto.getNombre(), e);
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al crear producto: " + e.getMessage(), e);
        }
        throw new RuntimeException("No se pudo obtener el ID del producto creado");
    }

    public void actualizar(Producto producto) {
        String sql = "UPDATE PRODUCTO SET NOMBRE = ?, DESCRIPCION = ?, PRECIO = ?, " +
                     "STOCK = ?, CATEGORIA = ?, IMAGEN_URL = ? WHERE ID_PRODUCTO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setBigDecimal(3, producto.getPrecio());
            ps.setInt(4, producto.getStock());
            ps.setString(5, producto.getCategoria());
            ps.setString(6, producto.getImagenUrl());
            ps.setInt(7, producto.getIdProducto());
            int filas = ps.executeUpdate();
            conn.commit();
            if (filas == 0) {
                throw new RuntimeException("Producto no encontrado");
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al actualizar producto ID: " + producto.getIdProducto(), e);
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
        }
    }

    public void eliminar(int idProducto) {
        String sql = "DELETE FROM PRODUCTO WHERE ID_PRODUCTO = ?";
        try (Connection conn = Conexion.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            int filas = ps.executeUpdate();
            conn.commit();
            if (filas == 0) {
                throw new RuntimeException("Producto no encontrado");
            }
        } catch (SQLException e) {
            Logger.registrar("ERROR", "Error al eliminar producto ID: " + idProducto, e);
            try { Conexion.getInstancia().getConexion().rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error al eliminar producto: " + e.getMessage(), e);
        }
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("ID_PRODUCTO"));
        p.setNombre(rs.getString("NOMBRE"));
        p.setDescripcion(rs.getString("DESCRIPCION"));
        p.setPrecio(rs.getBigDecimal("PRECIO"));
        p.setStock(rs.getInt("STOCK"));
        p.setCategoria(rs.getString("CATEGORIA"));
        p.setImagenUrl(rs.getString("IMAGEN_URL"));
        return p;
    }
}
