import java.util.List;

public class RelojParque implements Runnable {
    private final Parque parque;
    private final List<ActividadParque> actividades;
    private final JuegosDePremios premios;
    private final int cantidadEncargados;
    private final int totalDias;

    public RelojParque(Parque unParque, List<ActividadParque> unasActividades,
                       JuegosDePremios unosPremios, int unosEncargados, int cantDias) {
        parque = unParque;
        actividades = unasActividades;
        premios = unosPremios;
        cantidadEncargados = unosEncargados;
        totalDias = cantDias;
    }

    public void run() {
        try {
            for (int dia = 1; dia <= totalDias; dia++) {
                // 09:00 hs - Apertura del dia
                for (ActividadParque actividad : actividades) {
                    actividad.abrir(); // Prepara atracciones para una nueva tanda
                }
                parque.abrirDia(dia);
                Thread.sleep(3500); // Lapso diurno: 09:00 a 18:00 hs

                // 18:00 hs - Cierre de molinetes
                parque.cerrarIngreso();
                Thread.sleep(800);  // Lapso intermedio: 18:00 a 19:00 hs

                // 19:00 hs - Cierre de actividades
                parque.cerrarActividades();
                for (ActividadParque actividad : actividades) {
                    actividad.cerrar();
                }

                // 23:00 hs - Desalojo completo
                parque.esperarParqueVacio();
                Registro.informar("--- DIA " + dia + ": 23:00 hs - Parque desocupado. Cierre total de puertas ---");

                // Periodo nocturno (23:00 a 09:00 hs del dia siguiente)
                if (dia < totalDias) {
                    Registro.informar("--- NOCHE (Parque cerrado hasta las 09:00 hs) ---");
                    Thread.sleep(1500); // Tiempo en el que nuevos visitantes seran rechazados
                }
            }
            premios.cerrarEncargados(cantidadEncargados);
            Registro.informar("=================================================");
            Registro.informar(">>> SIMULACION COMPLETADA: " + totalDias + " DIAS CONCLUIDOS <<<");
            Registro.informar("=================================================");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar("Reloj del parque interrumpido.");
        }
    }
}
