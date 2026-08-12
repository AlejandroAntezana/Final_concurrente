import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class Teatro {
    private Semaphore capacidadSala;
    private CyclicBarrier grupoEntrada;

    public Teatro() {
        capacidadSala = new Semaphore(20, true);
        grupoEntrada = new CyclicBarrier(5, new Runnable() {
            public void run() {
                Registro.informar("TEATRO: ingresa un grupo completo de cinco personas");
            }
        });
    }

    public void verEspectaculo(String nombre) throws InterruptedException {
        capacidadSala.acquire();
        try {
            grupoEntrada.await();
            Registro.informar(nombre + " esta viendo el espectaculo");
            Thread.sleep(80);
        } catch (java.util.concurrent.BrokenBarrierException e) {
            Registro.informar("No pudo formarse un grupo del teatro");
        } finally {
            capacidadSala.release();
        }
    }
}
