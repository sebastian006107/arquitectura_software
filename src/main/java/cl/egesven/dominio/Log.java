package cl.egesven.dominio;

import java.time.LocalDateTime;

public class Log {
    private int idLog;
    private LocalDateTime fechaHora;
    private String tipo;
    private String mensaje;
    private String traza;

    public Log() {}

    public Log(int idLog, LocalDateTime fechaHora, String tipo, String mensaje, String traza) {
        this.idLog = idLog;
        this.fechaHora = fechaHora;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.traza = traza;
    }

    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTraza() { return traza; }
    public void setTraza(String traza) { this.traza = traza; }
}
