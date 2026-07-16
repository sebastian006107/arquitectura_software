package cl.egesven.dominio;

public class Cliente {
    private int idCliente;
    private int idUsuario;
    private String nombre;
    private String email;
    private String direccionPorDefecto;

    public Cliente() {}

    public Cliente(int idUsuario, String nombre, String email, String direccionPorDefecto) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.direccionPorDefecto = direccionPorDefecto;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccionPorDefecto() { return direccionPorDefecto; }
    public void setDireccionPorDefecto(String direccionPorDefecto) { this.direccionPorDefecto = direccionPorDefecto; }
}
