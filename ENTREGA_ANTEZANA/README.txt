Requisitos del Sistema

- Java Development Kit (JDK): Versión 8 o superior (recomendado JDK 11, 17 o 21).

- Consola / Terminal: Bash, Zsh, PowerShell o CMD.


Estructura de Archivos

.
├── ActividadParque.java       # Interfaz común polimórfica para atracciones y sectores
├── AutitosChocadores.java     # Atracción sincronizada con Semaphore y CyclicBarrier(20)
├── Comedor.java               # Salón comedor con Semaphore y CyclicBarrier(4)
├── EncargadoPremios.java      # Hilo consumidor de atención en juegos de premios
├── Ficha.java                 # Objeto de transferencia que encapsula los puntos
├── JuegosDePremios.java       # Sector de premios con BlockingQueue y Exchanger
├── MontanaRusa.java           # Montaña rusa con Semaphore (espera/asientos) y CyclicBarrier(5)
├── Parque.java                # Monitor central de control horario y aforo de molinetes
├── Premio.java                # Objeto de resultado entregado según puntaje
├── RealidadVirtual.java       # Actividad con ReentrantLock y Condition para kits compuestos
├── Registro.java              # Monitor estático para salida sincronizada por consola
├── RelojParque.java           # Hilo orquestador temporal de apertura y cierres
├── Shopping.java              # Sector comercial con Semaphore de aforo
├── SimulacionParque.java      # Clase principal ejecutable (punto de entrada / main)
├── SolicitudPremio.java       # Encapsula el Exchanger individual entre visitante y empleado
├── SolicitudViaje.java        # Encapsula el CountDownLatch individual por pasajero
├── Teatro.java                # Sala de espectáculos con Semaphore(20) y CyclicBarrier(5)
├── TransporteConCola.java     # Componente con BlockingQueue para Tren y Barco Pirata
└── Visitante.java             # Hilo autónomo de comportamiento no determinista

Instrucciones de Compilación y Ejecución

1. Limpieza y Compilación

Abra una terminal en la carpeta raíz donde se encuentran los archivos fuente .java y ejecute:

- En Linux / macOS:

rm -f *.class
javac *.java

- En Windows (CMD / PowerShell):

del *.class
javac *.java

2. Ejecución del Programa

Inicie la simulación ejecutando la clase principal:

java SimulacionParque

Configuración y Parámetros

Dentro de SimulacionParque.java es posible ajustar las constantes de ejecución para realizar las pruebas:

- CANTIDAD_VISITANTES: Número total de hilos de visitantes concurrentes (por defecto: 40).

- CANTIDAD_ENCARGADOS: Cantidad de empleados en el puesto de canje de premios (por defecto: 2).

En RelojParque.java, las pausas Thread.sleep() representan de forma escalada las horas del día del parque:

- 3000 ms: Jornada diurna con ingresos abiertos (09:00 a 18:00 hs).

- 500 ms: Lapso de actividades abiertas sin nuevos ingresos (18:00 a 19:00 hs).

- Espera pasiva hasta que personasDentro == 0 para anunciar el cierre total a las 23:00 hs.

Formato de Salida en Consola

Cada evento registrado en la salida estándar sigue el formato definido por Registro.java:

[   105 ms] [Visitante 3         ] Visitante 3 ingreso por un molinete
[   180 ms] [Visitante 12        ] Visitante 12 ocupo un lugar en los autitos
[   210 ms] [Conductor tren      ] TREN: parte con 8 pasajeros
[   450 ms] [Reloj               ] 19:00 - Cierran las actividades
[   720 ms] [Reloj               ] 23:00 - El parque esta vacio y cierra sus puertas

- [XXXX ms]: Milisegundos transcurridos desde el inicio de la simulación.

- [NombreHilo]: Identificador del hilo que ejecutó la acción.

- Mensaje: Detalle de la acción de sincronización o cambio de estado global.
