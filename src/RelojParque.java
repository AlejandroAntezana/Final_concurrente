public class RelojParque implements Runnable {
    private Parque parque;

    public RelojParque(Parque unParque) {
        parque = unParque;
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
