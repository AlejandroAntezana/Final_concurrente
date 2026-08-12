import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TransporteConCola implements Runnable {
    private String nombre;
    private int capacidad;
    private long esperaMaxima;
    private BlockingQueue fila;
    private volatile boolean funcionando;

    public TransporteConCola(String unNombre, int unaCapacidad, int capacidadFila,
                             long unaEsperaMaxima) {
        nombre = unNombre;
        capacidad = unaCapacidad;
        esperaMaxima = unaEsperaMaxima;
        fila = new ArrayBlockingQueue(capacidadFila, true);
        funcionando = true;
    }

    public boolean viajar(String visitante) throws InterruptedException {
        SolicitudViaje solicitud = new SolicitudViaje(visitante);
        if (!fila.offer(solicitud)) {
            Registro.informar(visitante + " encontro llena la fila de " + nombre);
            return false;
        }
        solicitud.esperarFin();
        return true;
    }

    public void cerrar() {
        funcionando = false;
    }

    public void run() {
        Registro.informar(nombre + ": hilo de servicio iniciado");
        try {
            while (funcionando || !fila.isEmpty()) {
                SolicitudViaje primero = (SolicitudViaje) fila.poll(50, TimeUnit.MILLISECONDS);
                if (primero != null) {
                    realizarViaje(primero);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar(nombre + ": hilo de servicio interrumpido");
        }
        Registro.informar(nombre + ": hilo de servicio finalizado");
    }

    private void realizarViaje(SolicitudViaje primero) throws InterruptedException {
        SolicitudViaje[] pasajeros = new SolicitudViaje[capacidad];
        pasajeros[0] = primero;
        int cantidad = 1;
        long limite = System.currentTimeMillis() + esperaMaxima;

        while (cantidad < capacidad) {
            long restante = limite - System.currentTimeMillis();
            if (restante <= 0) {
                break;
            }
            SolicitudViaje siguiente = (SolicitudViaje) fila.poll(restante, TimeUnit.MILLISECONDS);
            if (siguiente == null) {
                break;
            }
            pasajeros[cantidad] = siguiente;
            cantidad++;
        }

        Registro.informar(nombre + ": parte con " + cantidad + " pasajeros");
        Thread.sleep(80);
        int i;
        for (i = 0; i < cantidad; i++) {
            pasajeros[i].terminar();
        }
    }
}
