# Reporte de la solucion

## Modelo general

La simulacion usa un hilo por visitante y tambien hilos para el reloj, el conductor del tren, el
encargado del barco y los encargados de premios. Los objetos que representan recursos son
pasivos y protegen internamente su estado. Se eligieron 40 visitantes porque es multiplo de las
capacidades exactas de 4, 5 y 20, evitando dejar grupos incompletos al terminar la prueba.

`Parque` funciona como monitor. Sus metodos sincronizados y el patron `while-wait-notifyAll`
controlan la apertura del ingreso y permiten que el reloj espere hasta que no quede nadie. Tres
permisos de `Semaphore` representan los tres molinetes. A las 09:00 se habilita el ingreso, a las
18:00 se lo cierra, a las 19:00 cierran las actividades y a las 23:00 se verifica el parque vacio.
Los tiempos estan escalados para que la ejecucion dure pocos segundos.

## Mecanismos por area

- **Montana rusa:** una `CyclicBarrier` libera solamente grupos de cinco. Un semaforo de cinco
  permisos representa los asientos y se conserva durante el viaje, por lo que nadie sube antes
  de finalizar el turno anterior. Otro semaforo, adquirido con `tryAcquire`, limita el espacio de
  espera; quien lo encuentra lleno se va a otro sector y luego puede volver a intentarlo.
- **Autitos chocadores:** una barrera ciclica de 20 impide comenzar hasta completar diez autos
  con dos personas. Un semaforo limita la pista a esas 20 personas durante el turno.
- **Barco pirata:** una `ArrayBlockingQueue` forma la fila. El encargado toma hasta 20 personas
  y parte al llenarse o al vencer el tiempo maximo. Cada pasajero espera el fin mediante un
  `CountDownLatch` individual.
- **Juegos de premios:** una cola bloqueante conecta visitantes y encargados. Cada solicitud
  posee su propio `Exchanger`: en un encuentro se entrega la ficha y en otro se recibe un premio
  cuyo tamanio depende del puntaje. Un intercambiador por solicitud evita que dos empleados se
  emparejen accidentalmente entre si.
- **Comedor:** un semaforo representa la capacidad de tres mesas. Una barrera ciclica agrupa de
  a cuatro y nadie comienza a comer antes de que su mesa este completa. Se usa `tryAcquire` para
  que una persona pueda retirarse si el comedor esta lleno y volver a intentarlo mas tarde.
- **Tren:** utiliza una `BlockingQueue`, con el mismo esquema seguro de pedidos individuales del
  barco. Parte con diez pasajeros o al vencer la espera (cinco minutos simulados).
- **Teatro:** un semaforo limita la sala a 20 asistentes y una barrera ciclica hace ingresar cada
  grupo completo de cinco.
- **Realidad virtual:** un `ReentrantLock` justo y una variable `Condition` protegen las
  cantidades de visores, manoplas y bases. La guarda exige simultaneamente un visor, dos
  manoplas y una base; todos se reservan dentro de la misma seccion critica. Asi nunca se retiene
  un componente mientras se espera otro y se evita el deadlock.
- **Shopping:** un semaforo limita su ocupacion, de modo que tambien funciona como una opcion
  real dentro del recorrido del visitante.

## Seguridad y finalizacion

Los semaforos se liberan en bloques `finally`; los locks explicitos siguen la misma regla. Las
esperas de monitores y condiciones comprueban su guarda con `while`. Los equipos de VR se
toman de manera atomica, eliminando esperas circulares. Las colas atienden en orden justo y los
locks principales se crean justos, reduciendo la inanicion. Al finalizar los visitantes, el hilo
principal envia marcas de cierre a empleados, vacia las colas pendientes y hace `join` de todos
los hilos; por eso la JVM no queda con tareas abandonadas.

El codigo evita tipos genericos y colecciones de alto nivel no vistas: para grupos fijos utiliza
arreglos y, cuando el enunciado exige mecanismos concurrentes, emplea sus formas sin
parametrizacion junto con conversiones explicitas.
