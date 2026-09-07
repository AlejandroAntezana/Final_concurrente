import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class Teatro implements ActividadParque{
    private Semaphore capacidadSala;
    private CyclicBarrier grupoEntrada;
    private volatile boolean abierto;

    public Teatro() {
        capacidadSala = new Semaphore(20, true);
        abierto = true;
        grupoEntrada = new CyclicBarrier(5, new Runnable() {
            public void run() {
                Registro.informar("TEATRO: ingresa un grupo completo de cinco personas");
            }
        });
    }

    public boolean usar(String nombre) throws InterruptedException {
        capacidadSala.acquire();
        try {
            grupoEntrada.await();
            Registro.informar(nombre + " esta viendo el espectaculo");
            Thread.sleep(80);
            return true;
        } catch (java.util.concurrent.BrokenBarrierException e) {
            Registro.informar("No pudo formarse un grupo del teatro");
            return false;
        } finally {
            capacidadSala.release();
        }
    }

    public String getNombre(){
        return "Teatro";
    }

    public void cerrar(){
        abierto = false;
        grupoEntrada.reset();
    }
}
