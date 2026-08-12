import java.util.concurrent.Exchanger;

public class SolicitudPremio {
    private String visitante;
    private Exchanger intercambio;

    public SolicitudPremio(String unVisitante) {
        visitante = unVisitante;
        intercambio = new Exchanger();
    }

    public String getVisitante() {
        return visitante;
    }

    public Object intercambiar(Object objeto) throws InterruptedException {
        return intercambio.exchange(objeto);
    }
}
