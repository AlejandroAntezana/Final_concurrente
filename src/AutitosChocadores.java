import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class AutitosChocadores {
    private Semaphore lugares;
    private CyclicBarrier barrera;

    public AutitosChocadores() {
        lugares = new Semaphore(20, true);
        barrera = new CyclicBarrier(20, new Runnable() {
            public void run() {
                Registro.informar("AUTITOS: los 10 autos tienen dos personas; comienza el turno");
            }
        });
    }

    public void subir(String nombre) throws InterruptedException {
        lugares.acquire();
        try {
            Registro.informar(nombre + " ocupo un lugar en los autitos");
            barrera.await();
            Thread.sleep(70);
        } catch (java.util.concurrent.BrokenBarrierException e) {
            Registro.informar("Se interrumpio un turno de autitos");
        } finally {
            lugares.release();
        }
    }
}
