import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RealidadVirtual {
    private Lock control;
    private Condition equipoDisponible;
    private int visores;
    private int manoplas;
    private int bases;

    public RealidadVirtual(int cantidadVisores, int cantidadManoplas, int cantidadBases) {
        control = new ReentrantLock(true);
        equipoDisponible = control.newCondition();
        visores = cantidadVisores;
        manoplas = cantidadManoplas;
        bases = cantidadBases;
    }

    public void usar(String nombre) throws InterruptedException {
        tomarEquipo(nombre);
        try {
            Thread.sleep(60);
        } finally {
            devolverEquipo(nombre);
        }
    }

    private void tomarEquipo(String nombre) throws InterruptedException {
        control.lock();
        try {
            while (visores < 1 || manoplas < 2 || bases < 1) {
                equipoDisponible.await();
            }
            visores--;
            manoplas = manoplas - 2;
            bases--;
            Registro.informar(nombre + " recibio visor, dos manoplas y base de VR");
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
}
