public class Registro {
    private static final long INICIO = System.currentTimeMillis();

    public static synchronized void informar(String mensaje) {
        long transcurrido = System.currentTimeMillis() - INICIO;
        String hilo = Thread.currentThread().getName();
        System.out.printf("[%6d ms] [%-20s] %s%n", transcurrido, hilo, mensaje);
    }
}
