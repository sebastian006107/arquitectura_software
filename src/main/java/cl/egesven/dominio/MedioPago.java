package cl.egesven.dominio;

public class MedioPago {
    private int idMedioPago;
    private String tipo;
    private String estado;

    public MedioPago() {}

    public MedioPago(String tipo, String estado) {
        this.tipo = tipo;
        this.estado = estado;
    }

    public int getIdMedioPago() { return idMedioPago; }
    public void setIdMedioPago(int idMedioPago) { this.idMedioPago = idMedioPago; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return String.format("[%d] %s", idMedioPago, tipo);
    }
}
