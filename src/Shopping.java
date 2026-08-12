import java.util.concurrent.Semaphore;

public class Shopping {
    private Semaphore capacidad;

    public Shopping(int unaCapacidad) {
        capacidad = new Semaphore(unaCapacidad, true);
    }

    public void visitar(String nombre) throws InterruptedException {
        capacidad.acquire();
        try {
            Registro.informar(nombre + " esta recorriendo el shopping");
            Thread.sleep(30);
        } finally {
            capacidad.release();
        }
    }
}
