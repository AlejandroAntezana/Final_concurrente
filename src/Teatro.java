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
        // Chequeo previo: si cerro, no compite por capacidad
        if (!abierto) {
            return false;
        }
        capacidadSala.acquire();
        try {
            // Chequeo antes de trabarse en la barrera
            if (!abierto) {
                return false;
            }
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

    @Override
    public void abrir() {
        this.grupoEntrada = new CyclicBarrier(5); // Reinicia la barrera para el nuevo dia
        this.abierto = true;
        Registro.informar(" Teatro - habilitada para el publico.");
    }

    public String getNombre(){
        return "Teatro";
    }

    public void cerrar(){
        abierto = false;
        grupoEntrada.reset();
    }
}
