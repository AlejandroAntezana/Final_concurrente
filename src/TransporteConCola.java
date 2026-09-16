import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TransporteConCola implements Runnable, ActividadParque {
    private String nombre;
    private int capacidad;
    private long esperaMaxima;
    private BlockingQueue fila;
    private volatile boolean funcionando;
    private volatile boolean simulacionTerminada;

    public TransporteConCola(String unNombre, int unaCapacidad, int capacidadFila,
                             long unaEsperaMaxima) {
        nombre = unNombre;
        capacidad = unaCapacidad;
        esperaMaxima = unaEsperaMaxima;
        fila = new ArrayBlockingQueue(capacidadFila, true);
        funcionando = true;
        this.simulacionTerminada = false;
    }

    public String getNombre(){
        return nombre;
    }

    public boolean usar(String visitante) throws InterruptedException {
        if(!funcionando){
            return false;
        }
        SolicitudViaje solicitud = new SolicitudViaje(visitante);

        if (!fila.offer(solicitud)) {
            Registro.informar(visitante + " encontro llena la fila de " + nombre);
            return false;
        }
        solicitud.esperarFin();
        return true;
    }

    @Override
    public synchronized void abrir() {
        this.funcionando = true;
        Registro.informar(nombre + " habilito sus viajes para la jornada.");
        notifyAll(); // Despierta al conductor que esta en espera pasiva nocturna
    }

    public synchronized void cerrar() {
        funcionando = false;
        Registro.informar(nombre + " cerro sus ingresos. Conductor completando fila restante.");
    }

    public void run() {
        Registro.informar(nombre + ": Conductor inicio su servicio general.");
        try {
            while (!simulacionTerminada) {
                // 1. Espera pasiva nocturna hasta las 09:00 hs (no consume CPU)
                synchronized (this) {
                    while (!funcionando && !simulacionTerminada) {
                        wait();
                    }
                }

                if (simulacionTerminada) {
                    break;
                }

                // 2. Jornada diurna: atiende mientras este abierta O queden personas en la fila
                while (funcionando || !fila.isEmpty()) {
                    SolicitudViaje primero = (SolicitudViaje) fila.poll(100, TimeUnit.MILLISECONDS);
                    if (primero != null) {
                        realizarViaje(primero);
                    }
                }

                Registro.informar(nombre + ": Conductor finalizo la atencion del dia.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Registro.informar(nombre + ": Conductor finalizo su servicio definitivamente.");
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

    // Invocado al finalizar el ultimo dia para que el hilo conductor muera de forma limpia
    public synchronized void finalizarSimulacion() {
        this.simulacionTerminada = true;
        this.funcionando = false;
        notifyAll(); // Destraba al conductor del wait() nocturno para que salga de run()
    }
}
