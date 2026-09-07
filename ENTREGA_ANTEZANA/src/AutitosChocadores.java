import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class AutitosChocadores implements ActividadParque{
    private Semaphore lugares; //Lugares podria sser final?
    private CyclicBarrier barrera;
    private volatile boolean abierto;

    public AutitosChocadores() {
        lugares = new Semaphore(20, true);
        abierto = true;
        barrera = new CyclicBarrier(20, new Runnable() {
            public void run() {
                Registro.informar("AUTITOS: los 10 autos tienen dos personas; comienza el turno");
            }
        });
    }

    public boolean usar(String nombre) throws InterruptedException {
        if(!abierto){
            return false;
        }
        lugares.acquire();
        try {
            Registro.informar(nombre + " ocupo un lugar en los autitos");
            barrera.await();
            Thread.sleep(70);
            return true;
        } catch (java.util.concurrent.BrokenBarrierException e) {
            Registro.informar("Se interrumpio un turno de autitos");
            return false;
        } finally {
            lugares.release();
        }
    }

    public void cerrar() {
        abierto = false;
        barrera.reset();
    }

    @Override
    public String getNombre() {
        return "Autitos Chocadores";
    }
}
