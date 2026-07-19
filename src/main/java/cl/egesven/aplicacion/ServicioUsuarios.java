package cl.egesven.aplicacion;

import cl.egesven.dominio.Administrador;
import cl.egesven.dominio.Cliente;
import cl.egesven.dominio.UsuarioSistema;
import cl.egesven.infraestructura.Logger;
import cl.egesven.infraestructura.RepositorioClientes;

import java.security.MessageDigest;
import java.util.Base64;

public class ServicioUsuarios {

    private final RepositorioClientes repositorio;

    public ServicioUsuarios() {
        this.repositorio = new RepositorioClientes();
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(password.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear password: " + e.getMessage(), e);
        }
    }

    public static class Sesion {
        private int idUsuario;
        private String username;
        private String tipoUsuario;
        private Cliente cliente;
        private Administrador administrador;

        public boolean esCliente() { return "CLIENTE".equals(tipoUsuario); }
        public boolean esAdmin() { return "ADMINISTRADOR".equals(tipoUsuario); }

        public int getIdUsuario() { return idUsuario; }
        public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getTipoUsuario() { return tipoUsuario; }
        public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }
        public Cliente getCliente() { return cliente; }
        public void setCliente(Cliente cliente) { this.cliente = cliente; }
        public Administrador getAdministrador() { return administrador; }
        public void setAdministrador(Administrador administrador) { this.administrador = administrador; }
    }

    public Sesion login(String username, String password) {
        UsuarioSistema usuario = repositorio.buscarPorUsername(username);
        if (usuario == null) {
            Logger.registrar("ADVERTENCIA", "Login fallido: usuario inexistente '" + username + "'", (String) null);
            throw new RuntimeException("Usuario no encontrado");
        }
        if ("N".equals(usuario.getActivo())) {
            Logger.registrar("ADVERTENCIA", "Login rechazado: usuario desactivado '" + username + "'", (String) null);
            throw new RuntimeException("Usuario desactivado");
        }

        String hashIngresado = hashPassword(password);
        if (!hashIngresado.equals(usuario.getPasswordHash())) {
            Logger.registrar("ADVERTENCIA", "Login fallido: contrasena incorrecta para '" + username + "'", (String) null);
            throw new RuntimeException("Contrasena incorrecta");
        }

        Sesion sesion = new Sesion();
        sesion.setIdUsuario(usuario.getIdUsuario());
        sesion.setUsername(usuario.getUsername());
        sesion.setTipoUsuario(usuario.getTipoUsuario());

        if ("CLIENTE".equals(usuario.getTipoUsuario())) {
            Cliente cliente = repositorio.buscarClientePorIdUsuario(usuario.getIdUsuario());
            sesion.setCliente(cliente);
        } else if ("ADMINISTRADOR".equals(usuario.getTipoUsuario())) {
            Administrador admin = repositorio.buscarAdminPorIdUsuario(usuario.getIdUsuario());
            sesion.setAdministrador(admin);
        }

        return sesion;
    }

    public void registrarCliente(String username, String password, String nombre,
                                  String email, String direccion) {
        if (repositorio.existeUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }
        if (repositorio.existeEmail(email)) {
            throw new RuntimeException("El email ya esta registrado");
        }

        String hash = hashPassword(password);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);
        usuario.setPasswordHash(hash);
        usuario.setTipoUsuario("CLIENTE");

        int idUsuario = repositorio.registrarUsuario(usuario);

        Cliente cliente = new Cliente();
        cliente.setIdUsuario(idUsuario);
        cliente.setNombre(nombre);
        cliente.setEmail(email);
        cliente.setDireccionPorDefecto(direccion);

        repositorio.registrarCliente(cliente);
    }

    public void actualizarPasswordDemo(String username, String passwordPlano) {
        String hash = hashPassword(passwordPlano);
        repositorio.actualizarPasswordHash(username, hash);
    }
}
