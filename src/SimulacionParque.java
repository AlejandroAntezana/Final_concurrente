import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimulacionParque {
    private static final int CANTIDAD_VISITANTES_INICIALES = 40;
    private static final int CANTIDAD_ENCARGADOS = 2;
    private static final int CANTIDAD_DIAS = 3;

    public static void main(String[] args) {
        Thread.currentThread().setName("Simulacion principal");
        Registro.informar("Preparando los recursos y los hilos de la simulacion");

        Parque parque = new Parque(3);
        MontanaRusa montana = new MontanaRusa(10);
        AutitosChocadores autitos = new AutitosChocadores();
        TransporteConCola barco = new TransporteConCola("BARCO PIRATA", 20, 40, 600);
        JuegosDePremios premios = new JuegosDePremios(40);
        Comedor comedor = new Comedor(3);
        TransporteConCola tren = new TransporteConCola("TREN", 10, 30, 500);
        Teatro teatro = new Teatro();
        RealidadVirtual realidadVirtual = new RealidadVirtual(4, 6, 3);
        Shopping shopping = new Shopping(15);

        List<ActividadParque> actividades = new ArrayList(Arrays.asList(
                montana, autitos, barco, premios, comedor, tren, teatro, realidadVirtual, shopping
        ));

        Thread hiloReloj = new Thread(new RelojParque(parque, actividades, premios, CANTIDAD_ENCARGADOS, CANTIDAD_DIAS), "Reloj");
        Thread hiloBarco = new Thread(barco, "Encargado barco");
        Thread hiloTren = new Thread(tren, "Conductor tren");
        Thread[] encargados = new Thread[CANTIDAD_ENCARGADOS];
        Thread[] visitantes = new Thread[CANTIDAD_VISITANTES_INICIALES];

        int i;
        for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
            encargados[i] = new Thread(new EncargadoPremios(premios), "Encargado premios " + (i + 1));
        }
        for (i = 0; i < CANTIDAD_VISITANTES_INICIALES; i++) {
            visitantes[i] = new Thread(new Visitante("Visitante " + (i + 1), parque, actividades));
        }

        // Generador continuo de visitantes durante toda la simulacion
        Thread generadorVisitantes = new Thread(() -> {
            int id = CANTIDAD_VISITANTES_INICIALES + 1;
            try {
                while (hiloReloj.isAlive()) {
                    Thread.sleep(120); // Intervalo de llegada de nuevos visitantes

                    if (!hiloReloj.isAlive()) {
                        break;
                    }

                    Thread nuevo = new Thread(
                            new Visitante("Visitante " + id++, parque, actividades)
                    );
                    nuevo.start();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Generador visitantes");



        try {
            Registro.informar("Iniciando reloj, transportes y encargados");
            hiloReloj.start();
            hiloBarco.start();
            hiloTren.start();
            for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
                encargados[i].start();
            }

            Registro.informar("Iniciando " + CANTIDAD_VISITANTES_INICIALES + " visitantes");
            for (i = 0; i < CANTIDAD_VISITANTES_INICIALES; i++) {
                visitantes[i].start();
            }

            Registro.informar("Iniciando generador periodico de visitantes");
            generadorVisitantes.start();

            // Espera a que el reloj complete los 3 dias y el generador finalice
            hiloReloj.join();
            generadorVisitantes.join();

            barco.finalizarSimulacion();
            tren.finalizarSimulacion();

            // Espera la finalizacion ordenada de servicios y empleados
            hiloBarco.join();
            hiloTren.join();


           /* for (i = 0; i < CANTIDAD_VISITANTES_INICIALES; i++) {
                visitantes[i].join();
            }*/

            //hiloBarco.join();
            //hiloTren.join();
            for (i = 0; i < CANTIDAD_ENCARGADOS; i++) {
                encargados[i].join();
            }
            //hiloReloj.join();
            Registro.informar("Todos los hilos finalizaron. Simulacion terminada correctamente");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar("El hilo principal fue interrumpido");
        }
    }
}
