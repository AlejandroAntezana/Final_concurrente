import java.util.Random;

public class Visitante implements Runnable {
    private String nombre;
    private Parque parque;
    private MontanaRusa montana;
    private AutitosChocadores autitos;
    private TransporteConCola barco;
    private JuegosDePremios premios;
    private Comedor comedor;
    private TransporteConCola tren;
    private Teatro teatro;
    private RealidadVirtual realidadVirtual;
    private Shopping shopping;
    private Random azar;

    public Visitante(String unNombre, Parque unParque, MontanaRusa unaMontana,
                     AutitosChocadores unosAutitos, TransporteConCola unBarco,
                     JuegosDePremios unosPremios, Comedor unComedor,
                     TransporteConCola unTren, Teatro unTeatro,
                     RealidadVirtual unaRealidadVirtual, Shopping unShopping) {
        nombre = unNombre;
        parque = unParque;
        montana = unaMontana;
        autitos = unosAutitos;
        barco = unBarco;
        premios = unosPremios;
        comedor = unComedor;
        tren = unTren;
        teatro = unTeatro;
        realidadVirtual = unaRealidadVirtual;
        shopping = unShopping;
        azar = new Random();
    }

    public void run() {
        Thread.currentThread().setName(nombre);
        Registro.informar("Inicio su recorrido y espera la apertura del parque");
        boolean adentro = false;
        try {
            adentro = parque.ingresar(nombre);
            if (adentro) {
                /* Todos prueban las actividades; las que tienen espera limitada pueden rechazarlos. */
                shopping.visitar(nombre);
                while (!montana.subir(nombre)) {
                    /* Si la espera esta llena, recorre otro sector y vuelve a intentarlo. */
                    Thread.sleep(20);
                }
                autitos.subir(nombre);
                barco.viajar(nombre);
                premios.jugar(nombre, azar.nextInt(101));
                while (!comedor.comer(nombre)) {
                    Thread.sleep(20);
                }
                while (!tren.viajar(nombre)) {
                    Thread.sleep(20);
                }
                teatro.verEspectaculo(nombre);
                realidadVirtual.usar(nombre);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (adentro) {
                parque.salir(nombre);
            }
            Registro.informar("Finalizo su ejecucion");
        }
    }
}
