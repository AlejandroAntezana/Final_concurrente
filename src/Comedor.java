import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class Comedor {
    private Semaphore capacidad;
    private CyclicBarrier mesa;

    public Comedor(int cantidadMesas) {
        capacidad = new Semaphore(cantidadMesas * 4, true);
        mesa = new CyclicBarrier(4, new Runnable() {
            public void run() {
                Registro.informar("COMEDOR: se completo una mesa de cuatro; todos comienzan a comer");
            }
        });
    }

    public boolean comer(String nombre) throws InterruptedException {
        if (!capacidad.tryAcquire()) {
            Registro.informar(nombre + " encontro lleno el comedor y decidio volver luego");
            return false;
        }
        try {
            Registro.informar(nombre + " se sento en una mesa");
            mesa.await();
            Thread.sleep(50);
        } catch (java.util.concurrent.BrokenBarrierException e) {
            return false;
        } finally {
            capacidad.release();
        }
        return true;
    }
}
