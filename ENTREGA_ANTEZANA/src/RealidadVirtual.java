import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RealidadVirtual implements ActividadParque{
    private Lock control;
    private Condition equipoDisponible;
    private int visores;
    private int manoplas;
    private int bases;
    private volatile boolean abierto;

    public RealidadVirtual(int cantidadVisores, int cantidadManoplas, int cantidadBases) {
        control = new ReentrantLock(true);
        equipoDisponible = control.newCondition();
        visores = cantidadVisores;
        manoplas = cantidadManoplas;
        bases = cantidadBases;
        abierto = true;
    }

    public String getNombre() {
        return "Realidad Virtual";
    }

    public boolean usar(String nombre) throws InterruptedException {
        if (!tomarEquipo(nombre)) {
            return false;
        }
        try {
            Thread.sleep(60);
            return true;
        } finally {
            devolverEquipo(nombre);
        }
    }

    private boolean tomarEquipo(String nombre) throws InterruptedException {
        control.lock();
        try {
            while (visores < 1 || manoplas < 2 || bases < 1) {
                equipoDisponible.await();
            }
            if (!abierto) {
                return false;
            }
            visores--;
            manoplas = manoplas - 2;
            bases--;
            Registro.informar(nombre + " recibio visor, dos manoplas y base de VR");
            return true;
        } finally {
            control.unlock();
        }
    }

    private void devolverEquipo(String nombre) {
        control.lock();
        try {
            visores++;
            manoplas = manoplas + 2;
            bases++;
            Registro.informar(nombre + " devolvio el equipo completo de VR");
            equipoDisponible.signalAll();
        } finally {
            control.unlock();
        }
    }


    public void cerrar() {
        control.lock();
        try {
            abierto = false;
            equipoDisponible.signalAll();
        } finally {
            control.unlock();
        }
    }
}
