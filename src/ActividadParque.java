public interface ActividadParque {
 /*
 * Creo esta interfaz para generalizar algunos comportamientos de las actividades del parque,
 * para poder implementar el cierre de actividades y poder crear una lista de actividades que pueda
 * manipular de forma mas sencilla y sea escalable a mas atracciones
 * */
 boolean usar(String nombreVisitante) throws InterruptedException;

 void cerrar();

 String getNombre();
}
