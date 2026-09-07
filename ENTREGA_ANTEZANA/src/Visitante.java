import java.util.List;
import java.util.Random;

public class Visitante implements Runnable {
    private final String nombre;
    private final Parque parque;
    private final List<ActividadParque> actividades; //MEJORA :)
    private final Random azar;

    public Visitante(String unNombre, Parque unParque, List<ActividadParque> unasActividades) {
        nombre = unNombre;
        parque = unParque;
        actividades = unasActividades;
        azar = new Random();
    }

    public void run() {
        Thread.currentThread().setName(nombre);
        Registro.informar("Espera la apertura del parque");
        boolean adentro = false;
        try {
            adentro = parque.ingresar(nombre);
            if (adentro) {
                while (parque.estanAbiertasLasActividades()) {
                    int indice = azar.nextInt(actividades.size());
                    ActividadParque actividad = actividades.get(indice);
                    actividad.usar(nombre);
                    Thread.sleep(azar.nextInt(30) + 10);
                }
                Registro.informar(nombre + " se dirige a la salida tras el cierre de actividades");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (adentro) {
                parque.salir(nombre);
            }
            Registro.informar("Finalizo su recorrido");
        }
    }
}