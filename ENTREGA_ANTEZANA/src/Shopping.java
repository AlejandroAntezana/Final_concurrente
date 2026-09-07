import java.util.concurrent.Semaphore;

public class Shopping implements ActividadParque{
    private Semaphore capacidad;
    private volatile boolean abierto;

    public Shopping(int unaCapacidad) {
        abierto = true;
        capacidad = new Semaphore(unaCapacidad, true);
    }

    public String getNombre() {
        return "Shopping";
    }

    public boolean usar(String nombre) throws InterruptedException {
        if (!abierto) {
            return false;
        }
        capacidad.acquire();
        try {
            Registro.informar(nombre + " esta recorriendo el shopping");
            Thread.sleep(30);
            return true;
        } finally {
            capacidad.release();
        }
    }

    public void cerrar() {
        abierto = false;
    }
}
