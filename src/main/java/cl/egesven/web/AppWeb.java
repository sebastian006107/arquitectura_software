package cl.egesven.web;

import cl.egesven.aplicacion.*;
import cl.egesven.dominio.*;
import cl.egesven.infraestructura.Conexion;
import cl.egesven.infraestructura.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class AppWeb {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ServicioUsuarios servicioUsuarios = new ServicioUsuarios();
    private static final ServicioMFA servicioMFA = new ServicioMFA();
    private static final ServicioProductos servicioProductos = new ServicioProductos();
    private static final ServicioCarrito servicioCarrito = new ServicioCarrito();
    private static final ServicioPedidos servicioPedidos = new ServicioPedidos();
    private static ServicioUsuarios.Sesion sesion;

    public static void main(String[] args) {
        actualizarHashesDemo();

        staticFiles.location("/public");
        port(4567);

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // ── Auth ────────────────────────────────────────
        post("/api/login", (req, res) -> {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            res.type("application/json");
            try {
                sesion = servicioUsuarios.login(username, password);
                servicioMFA.generarYEnviar(username);
                return gson.toJson(Map.of("ok", true, "mensaje", "MFA enviado", "username", username,
                        "codigo", servicioMFA.getCodigoActual()));
            } catch (RuntimeException e) {
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        post("/api/mfa", (req, res) -> {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String codigo = body.get("codigo");
            res.type("application/json");
            if (servicioMFA.validar(codigo) && sesion != null) {
                return gson.toJson(Map.of("ok", true, "tipo", sesion.getTipoUsuario(),
                        "nombre", sesion.getCliente() != null ? sesion.getCliente().getNombre()
                                : sesion.getAdministrador().getNombre()));
            }
            sesion = null;
            return gson.toJson(Map.of("ok", false, "error", "Codigo incorrecto"));
        });

        // ── Registro ──────────────────────────────────
        post("/api/registro", (req, res) -> {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            res.type("application/json");
            try {
                servicioUsuarios.registrarCliente(
                        body.get("username"), body.get("password"),
                        body.get("nombre"), body.get("email"),
                        body.get("direccion"));
                return gson.toJson(Map.of("ok", true));
            } catch (RuntimeException e) {
                return gson.toJson(Map.of("ok", false, "error", mensajeRegistroAmigable(e.getMessage())));
            }
        });

        post("/api/logout", (req, res) -> {
            sesion = null;
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        get("/api/sesion", (req, res) -> {
            res.type("application/json");
            if (sesion == null) return gson.toJson(Map.of("activo", false));
            return gson.toJson(Map.of("activo", true, "tipo", sesion.getTipoUsuario(),
                    "nombre", sesion.esCliente() ? sesion.getCliente().getNombre()
                            : sesion.getAdministrador().getNombre()));
        });

        // ── Productos ──────────────────────────────────
        get("/api/productos", (req, res) -> {
            res.type("application/json");
            return gson.toJson(servicioProductos.listarProductos());
        });

        post("/api/productos", (req, res) -> {
            if (sesion == null || !sesion.esAdmin()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            servicioProductos.crearProducto(
                    (String) body.get("nombre"), (String) body.get("descripcion"),
                    new BigDecimal(body.get("precio").toString()),
                    ((Double) body.get("stock")).intValue(),
                    (String) body.get("categoria"),
                    (String) body.get("imagenUrl"));
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        put("/api/productos/:id", (req, res) -> {
            if (sesion == null || !sesion.esAdmin()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            int id = Integer.parseInt(req.params(":id"));
            servicioProductos.actualizarProducto(id,
                    (String) body.get("nombre"), (String) body.get("descripcion"),
                    new BigDecimal(body.get("precio").toString()),
                    ((Double) body.get("stock")).intValue(),
                    (String) body.get("categoria"),
                    (String) body.get("imagenUrl"));
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        delete("/api/productos/:id", (req, res) -> {
            if (sesion == null || !sesion.esAdmin()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            servicioProductos.eliminarProducto(Integer.parseInt(req.params(":id")));
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        // ── Carrito ────────────────────────────────────
        get("/api/carrito", (req, res) -> {
            if (sesion == null || !sesion.esCliente()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            res.type("application/json");
            Carrito c = servicioCarrito.obtenerCarrito(sesion.getCliente().getIdCliente());
            return gson.toJson(Map.of(
                    "items", c.getItems(),
                    "subtotal", c.calcularSubtotal(),
                    "impuesto", c.calcularImpuesto(),
                    "envio", c.calcularCostoEnvio(),
                    "total", c.calcularTotal()));
        });

        post("/api/carrito", (req, res) -> {
            if (sesion == null || !sesion.esCliente()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            int idProducto = ((Double) body.get("idProducto")).intValue();
            int cantidad = ((Double) body.get("cantidad")).intValue();
            servicioCarrito.agregarProducto(sesion.getCliente().getIdCliente(), idProducto, cantidad);
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        delete("/api/carrito/:id", (req, res) -> {
            if (sesion == null || !sesion.esCliente()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            servicioCarrito.quitarProducto(Integer.parseInt(req.params(":id")));
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        // ── Compra ─────────────────────────────────────
        post("/api/comprar", (req, res) -> {
            if (sesion == null || !sesion.esCliente()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String medioPago = body.get("medioPago");
            String direccion = body.getOrDefault("direccion", sesion.getCliente().getDireccionPorDefecto());
            try {
                Pedido pedido = servicioPedidos.procesarCompra(
                        sesion.getCliente().getIdCliente(), direccion, medioPago);
                res.type("application/json");
                return gson.toJson(Map.of("ok", true, "idPedido", pedido.getIdPedido(),
                        "total", pedido.getTotal(), "estado", pedido.getEstado()));
            } catch (RuntimeException e) {
                res.type("application/json");
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        // ── Pedidos ────────────────────────────────────
        get("/api/pedidos", (req, res) -> {
            if (sesion == null) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            res.type("application/json");
            if (sesion.esAdmin()) {
                return gson.toJson(servicioPedidos.listarTodosPedidos());
            }
            return gson.toJson(servicioPedidos.listarPedidosCliente(sesion.getCliente().getIdCliente()));
        });

        get("/api/pedidos/:id", (req, res) -> {
            if (sesion == null) { res.status(403); return gson.toJson(Map.of("error", "No autorizado")); }
            res.type("application/json");
            return gson.toJson(servicioPedidos.verPedido(Integer.parseInt(req.params(":id"))));
        });

        put("/api/pedidos/:id/estado", (req, res) -> {
            if (sesion == null || !sesion.esAdmin()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            servicioPedidos.actualizarEstadoPedido(
                    Integer.parseInt(req.params(":id")), body.get("estado"));
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        // ── Logs (admin) ───────────────────────────────
        get("/api/logs", (req, res) -> {
            if (sesion == null || !sesion.esAdmin()) {
                res.status(403);
                return gson.toJson(Map.of("error", "No autorizado"));
            }
            res.type("application/json");
            return gson.toJson(Logger.leerUltimos(50));
        });

        System.out.println("eGESVEN Web corriendo en http://localhost:4567");
    }

    private static String mensajeRegistroAmigable(String mensaje) {
        if (mensaje == null) {
            return "No se pudo completar el registro. Intenta nuevamente.";
        }
        String m = mensaje.toUpperCase();
        if (m.contains("UQ_USUARIO_USERNAME") || m.contains("USERNAME")) {
            return "Ese nombre de usuario ya esta en uso. Elige otro.";
        }
        if (m.contains("EMAIL")) {
            return "Ese email ya esta registrado. Usa otro o inicia sesion.";
        }
        if (m.contains("ORA-")) {
            return "No se pudo completar el registro. Revisa los datos e intenta nuevamente.";
        }
        return mensaje;
    }

    private static void actualizarHashesDemo() {
        try {
            servicioUsuarios.actualizarPasswordDemo("admin", "admin123");
            servicioUsuarios.actualizarPasswordDemo("jperez", "cliente123");
        } catch (Exception ignored) {}
    }
}
