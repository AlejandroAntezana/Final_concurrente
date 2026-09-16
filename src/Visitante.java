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
        /*Thread.currentThread().setName(nombre);
        Registro.informar("Espera la apertura del parque");
        boolean adentro = false;*/
        try {
            // Si el parque o los molinetes estan cerrados, finaliza su ejecucion aqui mismo
            if (!parque.ingresar(nombre)) {
                return;
            }


            try {

                    while (parque.estanAbiertasLasActividades()) {
                        // Genera un entero entre 0 y actividades.size() inclusive:
                        // De 0 a (size - 1) son actividades; el valor igual a size representa salir
                        int opcion = azar.nextInt(actividades.size()+1);

                        if (opcion == actividades.size()) {
                            Registro.informar(nombre + " decidio finalizar su recorrido y marcharse.");
                            break; // Sale voluntariamente del bucle de paseos
                        }
                        // Elige y disfruta la atraccion
                        ActividadParque actividad = actividades.get(opcion);
                        actividad.usar(nombre);
                        Thread.sleep(azar.nextInt(30) + 10);
                    }
                    Registro.informar(nombre + " se dirige a la salida tras el cierre de actividades");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                parque.salir(nombre);
                Registro.informar("Finalizo su recorrido");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar(nombre + " fue interrumpido durante su visita.");
        }
    }
}