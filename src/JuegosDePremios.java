import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class JuegosDePremios {
    private BlockingQueue solicitudes;
    private volatile boolean abierto;

    public JuegosDePremios(int capacidadFila) {
        solicitudes = new ArrayBlockingQueue(capacidadFila, true);
        abierto = true;
    }

    public void jugar(String visitante, int puntos) throws InterruptedException {
        SolicitudPremio solicitud = new SolicitudPremio(visitante);
        solicitudes.put(solicitud);
        /* Primer encuentro: entrega de la ficha. Segundo: recepcion del premio. */
        solicitud.intercambiar(new Ficha(puntos));
        Premio premio = (Premio) solicitud.intercambiar(null);
        Registro.informar(visitante + " recibio un premio " + premio.getTamanio()
                + " por obtener " + puntos + " puntos");
    }

    public SolicitudPremio tomarSolicitud() throws InterruptedException {
        return (SolicitudPremio) solicitudes.take();
    }

    public boolean estaAbierto() {
        return abierto;
    }

    public void cerrar(int cantidadEncargados) throws InterruptedException {
        abierto = false;
        int i;
        for (i = 0; i < cantidadEncargados; i++) {
            solicitudes.put(new SolicitudPremio("FIN"));
        }
    }
}
