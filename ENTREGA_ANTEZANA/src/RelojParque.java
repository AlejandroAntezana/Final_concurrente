import java.util.List;

public class RelojParque implements Runnable {
    private final Parque parque;
    private final List<ActividadParque> actividades;
    private final JuegosDePremios premios;
    private final int cantidadEncargados;

    public RelojParque(Parque unParque, List<ActividadParque> unasActividades,
                       JuegosDePremios unosPremios, int unosEncargados) {
        parque = unParque;
        actividades = unasActividades;
        premios = unosPremios;
        cantidadEncargados = unosEncargados;
    }

    public void run() {
        Registro.informar("Reloj del parque iniciado");
        try {
            Thread.sleep(100);
            parque.abrir();
            Thread.sleep(3000);
            parque.cerrarIngreso();
            Thread.sleep(500);
            parque.cerrarActividades();

            for (ActividadParque actividad : actividades) {
                actividad.cerrar();
            }
            premios.cerrarEncargados(cantidadEncargados);

            parque.esperarParqueVacio();
            Thread.sleep(300);
            Registro.informar("23:00 - El parque esta vacio y cierra sus puertas");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar("Reloj interrumpido");
        }
        Registro.informar("Reloj del parque finalizado");
    }
}
