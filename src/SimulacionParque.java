public class SimulacionParque {
    private static final int CANTIDAD_VISITANTES = 40;
    private static final int CANTIDAD_ENCARGADOS = 2;

    public static void main(String[] args) {
        Thread.currentThread().setName("Simulacion principal");
        Registro.informar("Preparando los recursos y los hilos de la simulacion");

        Parque parque = new Parque(3);
        MontanaRusa montana = new MontanaRusa(10);
        AutitosChocadores autitos = new AutitosChocadores();
        TransporteConCola barco = new TransporteConCola("BARCO PIRATA", 20, 40, 120);
        JuegosDePremios premios = new JuegosDePremios(40);
        Comedor comedor = new Comedor(3);
        TransporteConCola tren = new TransporteConCola("TREN", 10, 30, 50);
        Teatro teatro = new Teatro();
        RealidadVirtual realidadVirtual = new RealidadVirtual(4, 6, 3);
        Shopping shopping = new Shopping(15);

        Thread hiloReloj = new Thread(new RelojParque(parque), "Reloj");
        Thread hiloBarco = new Thread(barco, "Encargado barco");
        Thread hiloTren = new Thread(tren, "Conductor tren");
        Thread[] encargados = new Thread[CANTIDAD_ENCARGADOS];
        Thread[] visitantes = new Thread[CANTIDAD_VISITANTES];

        int i;
        for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
            encargados[i] = new Thread(new EncargadoPremios(premios),
                    "Encargado premios " + (i + 1));
        }
        for (i = 0; i < CANTIDAD_VISITANTES; i++) {
            visitantes[i] = new Thread(new Visitante("Visitante " + (i + 1), parque,
                    montana, autitos, barco, premios, comedor, tren, teatro,
                    realidadVirtual, shopping));
        }

        try {
            Registro.informar("Iniciando reloj, transportes y encargados");
            hiloReloj.start();
            hiloBarco.start();
            hiloTren.start();
            for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
                encargados[i].start();
            }
            Registro.informar("Iniciando " + CANTIDAD_VISITANTES + " visitantes");
            for (i = 0; i < CANTIDAD_VISITANTES; i++) {
                visitantes[i].start();
            }

            Registro.informar("Todos los hilos fueron iniciados; esperando a los visitantes");
            for (i = 0; i < CANTIDAD_VISITANTES; i++) {
                visitantes[i].join();
            }

            Registro.informar("Finalizaron los visitantes; deteniendo los hilos de servicio");
            barco.cerrar();
            tren.cerrar();
            premios.cerrar(CANTIDAD_ENCARGADOS);
            hiloBarco.join();
            hiloTren.join();
            for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
                encargados[i].join();
            }
            hiloReloj.join();
            Registro.informar("Todos los hilos finalizaron. Simulacion terminada correctamente");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar("El hilo principal fue interrumpido");
        }
    }
}
