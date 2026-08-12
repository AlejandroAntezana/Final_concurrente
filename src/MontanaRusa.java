import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MontanaRusa {
    private Semaphore lugarDeEspera;
    private Semaphore asientos;
    private CyclicBarrier barreraSalida;

    public MontanaRusa(int capacidadEspera) {
        lugarDeEspera = new Semaphore(capacidadEspera, true);
        asientos = new Semaphore(5, true);
        barreraSalida = new CyclicBarrier(5, new Runnable() {
            public void run() {
                Registro.informar("MONTANA RUSA: carro completo, comienza el viaje");
            }
        });
    }

    public boolean subir(String nombre) throws InterruptedException {
        if (!lugarDeEspera.tryAcquire()) {
            Registro.informar(nombre + " encontro llena la espera de la montana rusa y se fue");
            return false;
        }
        try {
            asientos.acquire();
            try {
                Registro.informar(nombre + " subio a la montana rusa");
                barreraSalida.await(2, TimeUnit.SECONDS);
                Thread.sleep(60);
                Registro.informar(nombre + " bajo de la montana rusa");
            } catch (java.util.concurrent.BrokenBarrierException e) {
                return false;
            } catch (java.util.concurrent.TimeoutException e) {
                barreraSalida.reset();
                return false;
            } finally {
                asientos.release();
            }
        } finally {
            lugarDeEspera.release();
        }
        return true;
    }
}
