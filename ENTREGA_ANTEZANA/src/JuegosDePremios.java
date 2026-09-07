import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Random;

public class JuegosDePremios implements ActividadParque{
    private final BlockingQueue solicitudes;
    private final Random azar;
    private volatile boolean abierto;

    public JuegosDePremios(int capacidadFila) {
        solicitudes = new ArrayBlockingQueue(capacidadFila, true);
        azar = new Random();
        abierto = true;
    }

    public String getNombre() {
        return "Juegos de Premios";
    }

    public boolean usar(String visitante) throws InterruptedException {
        if (!abierto) {
            return false;
        }
        int puntos = azar.nextInt(101);
        SolicitudPremio solicitud = new SolicitudPremio(visitante);
        if (!solicitudes.offer(solicitud)) {
            Registro.informar(visitante + " encontro lleno el puesto de juegos de premios");
            return false;
        }
        solicitud.intercambiar(new Ficha(puntos));
        Premio premio = (Premio) solicitud.intercambiar(null);
        Registro.informar(visitante + " recibio un premio " + premio.getTamanio()
                + " por obtener " + puntos + " puntos");
        return true;
    }

    public SolicitudPremio tomarSolicitud() throws InterruptedException {
        return (SolicitudPremio) solicitudes.take();
    }

    public void cerrar() {
        abierto = false;
    }

    public void cerrarEncargados(int cantidadEncargados) throws InterruptedException {
        for (int i = 0; i < cantidadEncargados; i++) {
            solicitudes.put(new SolicitudPremio("FIN"));
        }
    }
}
