import java.util.concurrent.CountDownLatch;

public class SolicitudViaje {
    private String visitante;
    private CountDownLatch fin;

    public SolicitudViaje(String unVisitante) {
        visitante = unVisitante;
        fin = new CountDownLatch(1);
    }

    public String getVisitante() {
        return visitante;
    }

    public void terminar() {
        fin.countDown();
    }

    public void esperarFin() throws InterruptedException {
        fin.await();
    }
}
