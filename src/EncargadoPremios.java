public class EncargadoPremios implements Runnable {
    private JuegosDePremios juegos;

    public EncargadoPremios(JuegosDePremios unosJuegos) {
        juegos = unosJuegos;
    }

    public void run() {
        Registro.informar("Inicio su turno en juegos de premios");
        try {
            boolean continuar = true;
            while (continuar) {
                SolicitudPremio solicitud = juegos.tomarSolicitud();
                if (solicitud.getVisitante().equals("FIN")) {
                    continuar = false;
                } else {
                    Premio premio = elegirPremio((Ficha) solicitud.intercambiar(null));
                    /* El segundo encuentro entrega el premio al visitante. */
                    solicitud.intercambiar(premio);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Registro.informar("Fue interrumpido mientras atendia");
        }
        Registro.informar("Finalizo su turno en juegos de premios");
    }

    private Premio elegirPremio(Ficha ficha) {
        if (ficha.getPuntos() >= 80) {
            return new Premio("grande");
        }
        if (ficha.getPuntos() >= 40) {
            return new Premio("mediano");
        }
        return new Premio("pequenio");
    }
}
