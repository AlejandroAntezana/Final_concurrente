import java.util.concurrent.Semaphore;

public class Parque {
    private Semaphore molinetes;
    private boolean ingresoAbierto;
    private boolean ingresoFinalizado;
    private boolean actividadesAbiertas;
    private int personasDentro;
    private int diaActual;

    public Parque(int cantidadMolinetes) {
        molinetes = new Semaphore(cantidadMolinetes, true);
        ingresoAbierto = false;
        ingresoFinalizado = false;
        actividadesAbiertas = false;
        personasDentro = 0;
        diaActual = 0;
    }

    // Invocado por RelojParque a las 09:00 hs de cada dia
    public synchronized void abrirDia(int dia) {
        this.diaActual = dia;
        this.ingresoAbierto = true;
        this.ingresoFinalizado = false;
        this.actividadesAbiertas = true;
        Registro.informar("=================================================");
        Registro.informar(">>> DIA " + diaActual + ": 09:00 hs - Parque abierto e ingresos habilitados <<<");
        Registro.informar("=================================================");
        notifyAll();
    }

    // Invocado por RelojParque a las 18:00 hs
    public synchronized void cerrarIngreso() {
        this.ingresoAbierto = false;
        this.ingresoFinalizado = true;
        Registro.informar("--- DIA " + diaActual + ": 18:00 hs - Molinetes cerrados. Fin de ingresos ---");
    }

    // Invocado por RelojParque a las 19:00 hs
    public synchronized void cerrarActividades() {
        this.actividadesAbiertas = false;
        Registro.informar("--- DIA " + diaActual + ": 19:00 hs - Actividades cerradas. Inicia evacuacion ---");
        notifyAll(); // Despierta a hilos que pudieran estar esperando en el monitor
    }

    public synchronized boolean estanAbiertasLasActividades() {
        return actividadesAbiertas;
    }

    public boolean ingresar(String nombre) throws InterruptedException {
        synchronized (this) {
            // 1. Si aun no abrio el parque (espera inicial del dia 1), esperan pasivamente
            while (!ingresoAbierto && !ingresoFinalizado) {
                Registro.informar(nombre + " llego antes de las 09:00 hs. Esperando apertura en la entrada...");
                wait();
            }

            // 2. Si ya pasaron las 18:00 hs o es de noche, rechazo inmediato
            if (ingresoFinalizado) {
                Registro.informar(nombre + " llego fuera de horario -> ACCESO DENEGADO.");
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

    public synchronized boolean actividadesAbiertas() {
        return actividadesAbiertas;
    }

    public synchronized int getDiaActual() {
        return diaActual;
    }
}
