import java.util.concurrent.Semaphore;

public class Parque {
    private Semaphore molinetes;
    private boolean ingresoAbierto;
    private boolean ingresoFinalizado;
    private boolean actividadesAbiertas;
    private int personasDentro;

    public Parque(int cantidadMolinetes) {
        molinetes = new Semaphore(cantidadMolinetes, true);
        ingresoAbierto = false;
        ingresoFinalizado = false;
        actividadesAbiertas = false;
        personasDentro = 0;
    }

    public synchronized void abrir() {
        ingresoAbierto = true;
        actividadesAbiertas = true;
        Registro.informar("09:00 - Abre el parque");
        notifyAll();
    }

    public synchronized void cerrarIngreso() {
        ingresoAbierto = false;
        ingresoFinalizado = true;
        Registro.informar("18:00 - Se cierra el ingreso al parque");
        notifyAll();
    }

    public synchronized void cerrarActividades() {
        actividadesAbiertas = false;
        Registro.informar("19:00 - Cierran las actividades");
        notifyAll();
    }

    public synchronized boolean estanAbiertasLasActividades() {
        return actividadesAbiertas;
    }

    public boolean ingresar(String nombre) throws InterruptedException {
        synchronized (this) {
            while (!ingresoAbierto && !ingresoFinalizado) {
                wait();
            }
            if (ingresoFinalizado) {
                return false;
            }
        }

        molinetes.acquire();

        try {
            Thread.sleep(10);
            synchronized (this) {
                personasDentro++;
                Registro.informar(nombre + " ingreso por un molinete");
            }
        } finally {
            molinetes.release();
        }
        return true;
    }

    public synchronized void salir(String nombre) {
        personasDentro--;
        Registro.informar(nombre + " salio del parque. Quedan " + personasDentro);
        notifyAll();
    }

    public synchronized void esperarParqueVacio() throws InterruptedException {
        while (personasDentro > 0) {
            wait();
        }
    }
}
