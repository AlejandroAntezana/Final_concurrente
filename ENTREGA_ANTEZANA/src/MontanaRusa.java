import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class MontanaRusa implements ActividadParque{
    private Semaphore lugarDeEspera;
    private Semaphore asientos;
    private CyclicBarrier barreraSalida;
    private volatile boolean abierto;

    public MontanaRusa(int capacidadEspera) {
        lugarDeEspera = new Semaphore(capacidadEspera, true);
        asientos = new Semaphore(5, true);
        abierto = true;
        barreraSalida = new CyclicBarrier(5, new Runnable() {
            public void run() {
                Registro.informar("MONTANA RUSA: carro completo, comienza el viaje");
            }
        });
    }

    public boolean usar(String nombre) throws InterruptedException {
        if (!abierto || !lugarDeEspera.tryAcquire()) {
            Registro.informar(nombre + " encontro llena la espera de la montana rusa y se fue");
            return false;
        }
        try {
            asientos.acquire(); // El visitante adquiere un asiento
        } finally {
            lugarDeEspera.release(); //Libera un lugar en la fila de esper al tomar un asiento
        }

        try {
            Registro.informar(nombre + " subio a la montana rusa");
            barreraSalida.await();
            Thread.sleep(60);
            Registro.informar(nombre + " bajo de la montana rusa");
        } catch (java.util.concurrent.BrokenBarrierException e) {
            return false;
        }finally {
            asientos.release();
        }

        return true;
    }

    public void cerrar(){
        abierto = false;
        barreraSalida.reset();
    }

    public String getNombre(){
        return "montana rusa";
    }
}
