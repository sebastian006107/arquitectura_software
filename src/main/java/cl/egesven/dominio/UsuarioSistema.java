package cl.egesven.dominio;

public class UsuarioSistema {
    private int idUsuario;
    private String username;
    private String passwordHash;
    private String tipoUsuario;
    private String activo;

    public UsuarioSistema() {}

    public UsuarioSistema(String username, String passwordHash, String tipoUsuario, String activo) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.tipoUsuario = tipoUsuario;
        this.activo = activo;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getActivo() { return activo; }
    public void setActivo(String activo) { this.activo = activo; }
}
