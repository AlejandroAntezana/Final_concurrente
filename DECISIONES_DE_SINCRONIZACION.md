# Parque de diversiones: decisiones de sincronización

> [!abstract] Propósito
> Esta nota explica cómo se resolvió cada consigna del trabajo, por qué se eligió cada
> mecanismo de sincronización, qué condiciones permiten usarlo y qué ocurriría si se
> intentara reemplazarlo por otro mecanismo sin adaptar el diseño.

## 1. Criterio general del modelo

La simulación aplica el modelo de **un hilo por objeto activo**:

- cada visitante es un `Thread`;
- el reloj del parque tiene su propio hilo;
- el barco y el tren tienen un hilo encargado de formar y ejecutar viajes;
- cada encargado de premios es un hilo consumidor.

Las atracciones son objetos pasivos compartidos. Cada una encapsula su estado y ofrece
operaciones seguras para los visitantes. De esta manera, `Visitante` expresa el recorrido
y cada recurso decide internamente quién puede entrar, cuándo debe esperar y cuándo debe
despertarse.

La elección de mecanismos no se hizo por variedad solamente. En el trabajo aparecen
problemas concurrentes diferentes:

| Necesidad | Mecanismo principal |
|---|---|
| Limitar una cantidad de recursos iguales | `Semaphore` |
| Esperar a que se forme un grupo exacto y reutilizar el punto de encuentro | `CyclicBarrier` |
| Mantener una fila bloqueante entre productores y un consumidor | `BlockingQueue` |
| Esperar la finalización de un único viaje | `CountDownLatch` |
| Intercambiar objetos entre exactamente dos participantes | `Exchanger` |
| Esperar una condición definida sobre varias variables | `ReentrantLock` + `Condition` |
| Proteger estado y esperar cambios sencillos del ciclo de vida | monitor con `synchronized`, `wait` y `notifyAll` |

> [!important] “Otro mecanismo sería incorrecto”
> En concurrencia casi nunca significa que una clase de Java sea universalmente
> incorrecta. Significa que **usarla sola** no representa la condición pedida, o que
> obliga a reconstruir manualmente una abstracción que Java ya ofrece. Por ejemplo, un
> semáforo limita a cinco personas, pero no garantiza por sí mismo que las cinco hayan
> llegado antes de empezar.

---

## 2. Entrada, horarios y salida del parque

Archivos relacionados: [[src/Parque.java|Parque]], [[src/RelojParque.java|RelojParque]]
y [[src/SimulacionParque.java|SimulacionParque]].

### 2.1 Molinetes: `Semaphore`

La entrada posee tres molinetes físicos. Se modelaron con:

```java
molinetes = new Semaphore(cantidadMolinetes, true);
```

Cada permiso equivale a un molinete. Antes de atravesarlo el visitante ejecuta
`acquire()` y al terminar ejecuta `release()` dentro de un `finally`.

#### Por qué corresponde

Un semáforo contador representa directamente un conjunto de recursos homogéneos:
no importa **qué** molinete se obtiene, sino que como máximo haya `k` visitantes
atravesándolos simultáneamente. Se cumplen las condiciones apropiadas para usarlo:

1. existe una capacidad máxima conocida;
2. cada usuario consume exactamente un permiso;
3. el permiso se usa durante un intervalo delimitado;
4. al terminar puede devolverse sin transferir datos ni coordinar un grupo.

El parámetro `true` solicita una política de atención aproximadamente FIFO para los
hilos que ya están esperando y reduce la posibilidad de inanición.

#### Alternativas

- Un contador dentro de un monitor podría resolverlo con `while`, `wait` y
  `notifyAll`, pero duplicaría manualmente el comportamiento de un semáforo.
- Un `ReentrantLock` solo daría exclusión mutua, es decir, permitiría un único
  visitante aunque existan tres molinetes. Harían falta además una `Condition` y un
  contador.
- Una `CyclicBarrier(3)` sería incorrecta para esta condición: obligaría a que siempre
  llegaran exactamente tres personas juntas. Los molinetes deben permitir también una
  o dos entradas.
- Una cola podría ordenar visitantes, pero por sí sola no limitaría cuántos están
  usando los molinetes.

### 2.2 Apertura, cierre y cantidad de personas: monitor

`Parque` usa métodos `synchronized` y bloques `synchronized (this)`. El monitor protege
en conjunto:

- `ingresoAbierto`;
- `ingresoFinalizado`;
- `actividadesAbiertas`;
- `personasDentro`.

Antes de las 09:00, `ingresar()` espera con esta guarda:

```java
while (!ingresoAbierto && !ingresoFinalizado) {
    wait();
}
```

Al abrir o cerrar, el reloj cambia el estado bajo el mismo monitor y llama a
`notifyAll()`. A las 23:00 simuladas, `esperarParqueVacio()` espera mientras
`personasDentro > 0`. Cada salida decrementa el contador y notifica.

#### Por qué corresponde

Es un caso adecuado para monitor porque varias operaciones cortas deben observar y
modificar un estado lógico coherente. La condición no es “hay N permisos”, sino
“el ingreso abrió o ya finalizó” y, para el reloj, “el contador llegó a cero”.

El `while` es indispensable: un hilo puede despertarse sin que su condición sea cierta,
o puede perder la carrera por el estado antes de recuperar el monitor. `if` no vuelve a
validar la guarda y por eso sería inseguro. `notifyAll()` se eligió porque sobre el mismo
monitor esperan clases lógicas distintas: visitantes esperando la apertura y el reloj
esperando que el parque quede vacío.

#### Alternativas

- `ReentrantLock` con una o dos `Condition` sería correcto y permitiría separar las
  colas de espera, pero el estado y las operaciones son pequeños; el monitor resulta
  más directo.
- Variables `volatile` darían visibilidad, pero no atomicidad para
  `personasDentro++/--` ni una operación de espera eficiente. Hacer *polling* consumiría
  CPU y seguiría expuesto a carreras.
- Un semáforo funciona bien para los molinetes, pero no expresa cómodamente todos los
  estados horarios ni la guarda “parque vacío”.

### 2.3 Ciclo de vida de los hilos: `join`

El hilo principal hace `join()` de todos los visitantes antes de enviar el cierre a los
empleados y transportes, y luego hace `join()` de estos. `join` expresa una relación de
finalización: el hilo llamador no continúa hasta que termine el hilo esperado.

Reemplazarlo por `sleep` sería incorrecto: dormir solo estima una duración y no prueba
que el otro hilo haya terminado. Un `CyclicBarrier` tampoco es natural aquí porque los
participantes no necesitan reutilizar un punto de encuentro.

> [!warning] Alcance real del horario
> `cerrarActividades()` cambia `actividadesAbiertas`, pero `Visitante` solo consulta el
> estado al ingresar y después completa todas las atracciones sin volver a comprobarlo.
> Por lo tanto, el código **anuncia** el cierre de actividades a las 19:00, pero no
> impide comenzar una actividad después de esa hora. Además, las 23:00 se informan
> solamente después de que el parque queda vacío: no existe una expulsión forzada.

---

## 3. Montaña rusa

Archivo: [[src/MontanaRusa.java|MontanaRusa]].

La solución combina tres responsabilidades:

1. `lugarDeEspera`, un semáforo, limita la zona de espera;
2. `asientos`, otro semáforo de cinco permisos, reserva los lugares del carro;
3. `barreraSalida`, una `CyclicBarrier(5)`, impide comenzar con menos de cinco.

### Por qué se necesitan mecanismos diferentes

La zona de espera y los asientos representan **capacidades**; la salida representa un
**encuentro grupal**. Un visitante intenta entrar a la espera mediante `tryAcquire()`.
Si no obtiene permiso, no se bloquea: informa que el sector está lleno, se va y puede
volver más tarde. Esto reproduce la opción indicada en el enunciado.

Después adquiere un asiento y lo conserva durante el viaje. Como solo existen cinco
permisos, un segundo grupo no puede subir hasta que el primero baje y libere los
asientos. La barrera, en cambio, hace que los cinco ocupantes esperen hasta completar el
carro. Al llegar el quinto se ejecuta la acción que anuncia el comienzo y la barrera se
abre para todo el grupo. Es cíclica porque debe reutilizarse en cada viaje.

Se cumplen las condiciones para `CyclicBarrier`:

- el tamaño del grupo es fijo;
- todos realizan la misma fase “llegar y esperar”;
- ninguno debe continuar antes del último;
- la coordinación se repite durante muchos viajes.

### Alternativas y por qué no alcanzan solas

- Solo un `Semaphore(5)` permite como máximo cinco pasajeros, pero el primero podría
  comenzar antes de que lleguen los otros cuatro.
- Solo una barrera no impide que miembros del turno siguiente se aproximen y se mezclen
  cuando los permisos del carro aún no fueron liberados.
- Un `CountDownLatch(5)` sirve una sola vez; habría que crear y publicar otro latch por
  cada viaje, lo cual exige administrar explícitamente las generaciones.
- Una `Phaser` sería válida, pero está pensada para cantidades dinámicas y múltiples
  fases. Aquí el grupo fijo hace más clara a `CyclicBarrier`.

La espera de la barrera tiene un límite de tiempo. Si no se completa el grupo, se hace
`reset()` para evitar que los ocupantes queden bloqueados indefinidamente.

> [!warning] Detalle de robustez
> Si un hilo se interrumpe durante `await`, la barrera queda rota y los demás reciben
> `BrokenBarrierException`, pero una llamada concurrente a `reset()` también puede
> afectar a la generación siguiente. Para una simulación controlada es suficiente; una
> implementación de producción debería encapsular cada viaje como una generación
> explícita bajo un lock.

---

## 4. Autitos chocadores

Archivo: [[src/AutitosChocadores.java|AutitosChocadores]].

Hay diez autos y dos personas por auto, por lo que cada turno exige veinte visitantes.
Se usa:

- `Semaphore(20, true)` para limitar la pista;
- `CyclicBarrier(20)` para que el turno no comience hasta completar los diez autos.

La justificación es la misma separación entre capacidad y encuentro grupal de la
montaña rusa. Los veinte permisos se conservan hasta finalizar el turno, por lo que no
entra el turno siguiente mientras el actual sigue usando la pista. La acción de barrera
anuncia que todos los autos están completos.

Un semáforo de diez permisos —uno por auto— sería una representación incorrecta si cada
visitante adquiriera uno, porque modelaría diez personas, no veinte. También sería
posible crear diez objetos `Auto`, cada uno con una barrera de dos, y después otra
barrera global de diez autos; esa solución representa mejor las parejas concretas, pero
añade complejidad innecesaria porque la consigna solo requiere veinte ocupantes totales.

Solo una barrera de veinte no protegería la pista durante el viaje; solo un semáforo de
veinte no esperaría a completar el turno. Un latch no es reutilizable entre turnos.

> [!warning] Interrupciones
> No hay tiempo máximo. Si no llegan veinte visitantes o uno es interrumpido, el resto
> puede quedar esperando o recibir una barrera rota. La simulación usa cuarenta
> visitantes precisamente para formar dos grupos completos.

---

## 5. Barco pirata

Archivos: [[src/TransporteConCola.java|TransporteConCola]] y
[[src/SolicitudViaje.java|SolicitudViaje]].

El barco reutiliza el componente `TransporteConCola` con capacidad veinte y una espera
máxima simulada. La arquitectura es productor–consumidor:

- los visitantes producen objetos `SolicitudViaje` mediante `offer`;
- una `ArrayBlockingQueue` mantiene la fila en orden;
- el encargado consume el primer pedido y completa un arreglo con hasta veinte;
- `poll(tiempoRestante, TimeUnit.MILLISECONDS)` espera pasajeros solo hasta el límite;
- cada solicitud contiene un `CountDownLatch(1)` para que su visitante espere el final.

### Por qué `BlockingQueue`

La consigna combina orden de llegada, capacidad de fila y espera bloqueante. Una cola
bloqueante resuelve esos tres aspectos sin implementar manualmente una lista compartida.
`ArrayBlockingQueue` tiene capacidad fija y se construye con equidad. `offer` permite que
el visitante detecte una fila llena sin quedar bloqueado.

El plazo comienza cuando el encargado obtiene al primer pasajero. Desde ese momento
parte al cumplirse una de dos condiciones:

```text
cantidad == capacidad  OR  se agotó esperaMaxima
```

Usar únicamente una barrera de veinte sería incorrecto porque no puede liberar un grupo
incompleto al vencer el plazo. Un semáforo limita plazas, pero no conserva una fila ni
permite que un encargado seleccione un lote ordenado. Un monitor podría implementar
todo, aunque obligaría a programar manualmente la cola y las esperas temporizadas.

### Por qué un latch individual

Después de ser seleccionado, cada visitante necesita esperar un evento único: el fin de
**su** viaje. `CountDownLatch(1)` representa exactamente esa transición. El encargado
llama `countDown()` una vez y `await()` retorna incluso si la señal ocurrió antes de que
el visitante comenzara a esperar.

Una `CyclicBarrier` no corresponde: el visitante y el conductor no están formando un
grupo que deba llegar simultáneamente a una fase. `wait/notify` sin una bandera sería
propenso a una notificación perdida. Un latch compartido entre viajes podría liberar
pasajeros de la generación equivocada; por eso cada solicitud posee el suyo.

> [!note] Relación con el enunciado
> Los pasajeros que no caben en el viaje actual permanecen en la cola para el siguiente,
> como pide el barco. Si se llena la capacidad total de la fila, `viajar` retorna
> `false`. En el recorrido actual, `Visitante` no reintenta el barco; por lo tanto, ese
> caso excepcional no garantiza esperar el siguiente viaje.

---

## 6. Juegos de premios

Archivos: [[src/JuegosDePremios.java|JuegosDePremios]],
[[src/SolicitudPremio.java|SolicitudPremio]] y
[[src/EncargadoPremios.java|EncargadoPremios]].

Aquí se combinan una `BlockingQueue` compartida y un `Exchanger` privado por solicitud.

1. El visitante encola su solicitud.
2. Un encargado la toma.
3. En el primer `exchange`, el visitante entrega una `Ficha`.
4. El encargado calcula el tamaño del premio según los puntos.
5. En el segundo `exchange`, el encargado entrega el `Premio`.

### Por qué se usa una cola

Hay muchos visitantes productores y dos encargados consumidores. La cola distribuye
cada solicitud a un solo encargado, bloquea eficientemente a los empleados cuando no
hay trabajo y aplica contrapresión si se llena. Una lista común requeriría lock,
condición “no vacía”, condición “no llena” y control manual de índices.

### Por qué se usa `Exchanger`

`Exchanger` está diseñado para que **dos hilos** se encuentren y cada uno reciba el
objeto aportado por el otro. Esta semántica coincide literalmente con el canje:
ficha por coordinación y luego premio por coordinación. El visitante no puede afirmar
que recibió el premio hasta que el encargado efectivamente lo entrega.

La condición crucial es que cada `SolicitudPremio` crea su propio intercambiador. Si
todos compartieran uno, un visitante podría emparejarse con otro visitante, o dos
encargados entre sí, intercambiando tipos y solicitudes incorrectas.

### Alternativas

- Dos `BlockingQueue`, una de fichas y otra de premios, podrían funcionar si cada
  mensaje llevara un identificador y existiera una forma segura de correlacionarlos.
- Un `Future` o `CompletableFuture` modelaría bien la devolución del premio, pero no
  mostraría de manera tan directa el intercambio bidireccional requerido.
- Un semáforo solo cuenta permisos; no transporta la ficha ni el premio.
- Una barrera sincroniza llegadas, pero tampoco transfiere el resultado correcto a un
  visitante específico.
- Un único slot con `wait/notify` puede implementarlo, aunque exige banderas y guardas
  para impedir sobrescrituras y despertares incorrectos.

Para cerrar a los encargados se insertan solicitudes centinela `"FIN"`, una por cada
empleado. Esto despierta a quienes estén bloqueados en `take()` y permite una
finalización ordenada. `volatile abierto` aporta visibilidad, aunque actualmente el
recorrido no consulta esa bandera antes de encolar.

---

## 7. Comedor

Archivo: [[src/Comedor.java|Comedor]].

El comedor tiene tres mesas de cuatro personas. La implementación usa:

- un semáforo de `cantidadMesas * 4` permisos para la capacidad total;
- una `CyclicBarrier(4)` para que nadie empiece a comer hasta formar un grupo de cuatro.

`tryAcquire()` implementa la decisión “si está lleno, se va y vuelve luego”. Una vez
sentado, el visitante espera en la barrera. Al llegar el cuarto, la acción anuncia el
comienzo y los cuatro continúan. La barrera se reutiliza para los grupos siguientes.

### Condiciones que justifican la barrera

- cada mesa lógica tiene tamaño fijo;
- todos ejecutan la misma etapa;
- el cuarto libera a los tres anteriores;
- habrá muchos grupos sucesivos.

Un semáforo de cuatro por sí solo solo limitaría cuatro plazas, pero permitiría que el
primero comiera sin esperar. Un latch de cuatro no puede reiniciarse para otra mesa.
Una cola puede agrupar visitantes, pero haría falta un hilo “mozo” consumidor que forme
cada lote y despierte a sus integrantes.

### Alcance del modelo

La barrera es global, no hay objetos `Mesa`. Funcionalmente crea lotes consecutivos de
cuatro, pero no identifica una mesa física específica. Esto es suficiente para la regla
“comenzar de a cuatro” mientras todos tardan aproximadamente lo mismo.

> [!warning] Posible bloqueo final
> Si la cantidad total de comensales no es múltiplo de cuatro, el último grupo queda
> esperando indefinidamente. Se eligieron cuarenta visitantes para evitarlo. Una
> solución general necesitaría una política de cierre, cancelación o tiempo máximo.

---

## 8. Tren turístico

Archivos: [[src/TransporteConCola.java|TransporteConCola]] y
[[src/SolicitudViaje.java|SolicitudViaje]].

El tren usa el mismo patrón productor–consumidor del barco, configurado con capacidad
diez. La cola conserva el orden, el conductor forma un lote y `poll` temporizado hace
que parta al llenarse o al expirar la espera, lo que ocurra primero. Los cinco minutos
del enunciado están escalados a milisegundos para no prolongar la prueba.

Una barrera de diez no permitiría la partida por tiempo con menos pasajeros. Un
`ScheduledExecutorService` podría programar la salida, pero todavía harían falta una
cola, exclusión mutua y una forma de cancelar la tarea si el tren se llena antes. La
espera temporizada de `BlockingQueue` mantiene la decisión en un único hilo conductor y
reduce las carreras entre “se llenó” y “venció el tiempo”.

Al terminar el paseo, los latches individuales despiertan exactamente a los pasajeros
seleccionados. Quienes quedaron en la cola pertenecen al próximo viaje.

---

## 9. Teatro

Archivo: [[src/Teatro.java|Teatro]].

El teatro combina:

- `Semaphore(20, true)` para la capacidad total de la sala;
- `CyclicBarrier(5)` para formar grupos de ingreso completos.

Son dos restricciones independientes. El semáforo asegura que nunca haya más de veinte
personas viendo el espectáculo. La barrera asegura que el acceso se produzca en lotes
de cinco. Como caben cuatro grupos y la barrera es reutilizable, cada quinta llegada
abre una nueva generación.

Un `Semaphore(5)` no resolvería la capacidad veinte; cuatro semáforos de cinco
necesitarían además asignar cada visitante a un grupo. Un único `Semaphore(20)` tampoco
obliga a entrar de a cinco. Un latch no sirve para grupos sucesivos sin reconstruirlo.

> [!warning] Modelo temporal simplificado
> No existe un hilo que programe funciones concretas ni una compuerta que abra cuando
> “comienza el espectáculo”. El código modela capacidad, grupos y duración, pero no la
> periodicidad mencionada en la consigna. También requiere que el total que llegue sea
> múltiplo de cinco para evitar un grupo final incompleto.

---

## 10. Realidad virtual

Archivo: [[src/RealidadVirtual.java|RealidadVirtual]].

Cada visitante necesita adquirir atómicamente:

- un visor;
- dos manoplas;
- una base.

Las existencias se protegen con un `ReentrantLock(true)` y los visitantes que no pueden
formar un equipo esperan en una `Condition`:

```java
while (visores < 1 || manoplas < 2 || bases < 1) {
    equipoDisponible.await();
}
```

Cuando la guarda es falsa se descuentan los cuatro componentes dentro de la misma
sección crítica. Al terminar, se devuelven juntos y se ejecuta `signalAll()`.

### Por qué `Lock` y `Condition`

La disponibilidad es una expresión sobre **tres contadores relacionados**. Revisar la
guarda y descontar existencias debe ser una única operación atómica; de lo contrario,
dos visitantes podrían observar el mismo equipo y reservarlo dos veces.

`Condition` permite dormir sin mantener el lock: `await()` libera el lock
atómicamente, y lo recupera antes de retornar. El `while` vuelve a validar la guarda
porque otro visitante puede quedarse con el equipo antes de que el hilo despertado
recupere el lock. `signalAll()` es apropiado porque una devolución puede habilitar a
varios visitantes y no se sabe cuál podrá satisfacer la guarda.

El lock justo reduce la inanición. No ofrece una garantía matemática absoluta de orden
en todas las circunstancias, pero favorece al hilo que lleva más tiempo esperando.

### Cómo se evita el deadlock

La regla principal es **todo o nada**: nadie retiene un visor mientras espera manoplas,
ni retiene manoplas mientras espera una base. Si el equipo no está completo, no se
descuenta ningún recurso. Así se elimina la condición de “retener y esperar”, necesaria
para que exista una espera circular.

### Alternativas y riesgos

- Tres semáforos independientes parecen naturales, pero adquirirlos sucesivamente puede
  producir retención parcial. Por ejemplo, varios visitantes podrían tomar todos los
  visores y luego esperar manoplas mientras otros recursos quedan inutilizados. Podría
  diseñarse una adquisición ordenada con devolución ante fallo, pero sería más compleja
  y susceptible a inanición.
- Un único semáforo con la cantidad de equipos completos funcionaría solamente si las
  proporciones fueran fijas para siempre. Ocultaría inventarios diferentes y no
  representaría correctamente que las manoplas se consumen de a dos.
- Un monitor con `synchronized`, `wait` y `notifyAll` también sería correcto. Se eligió
  `ReentrantLock` para aplicar explícitamente el mecanismo solicitado y disponer de
  equidad y una condición asociada.
- Una barrera no corresponde: no se espera un número fijo de visitantes, sino que se
  espera disponibilidad de recursos.

Los métodos liberan el lock en `finally`, y `usar` devuelve el equipo también en
`finally`; por eso una interrupción durante la actividad no pierde los componentes.

---

## 11. Shopping

Archivo: [[src/Shopping.java|Shopping]].

Aunque el enunciado no impone una sincronización especial, el shopping es un recurso de
capacidad limitada. Un `Semaphore` justo representa las plazas disponibles. El
visitante bloquea con `acquire`, recorre el lugar y libera en `finally`.

No hace falta una barrera porque los visitantes no deben entrar en grupo ni empezar al
mismo tiempo. Tampoco hace falta una cola explícita: el semáforo ya mantiene hilos
esperando por permisos. Un lock exclusivo sería demasiado restrictivo, porque admitiría
solo una persona.

---

## 12. Registro de mensajes

Archivo: [[src/Registro.java|Registro]].

`Registro.informar` es `static synchronized`. Esto serializa las llamadas a
`System.out.println` y hace que cada evento de la simulación se imprima como una unidad.
El lock utilizado es el monitor de la clase `Registro`.

No ordena causalmente toda la simulación: dos eventos independientes pueden aparecer en
distinto orden entre ejecuciones. Eso es esperable en un programa concurrente. Su
objetivo es evitar que varias operaciones de registro propias se ejecuten
simultáneamente.

Una `BlockingQueue` de eventos y un hilo registrador sería una alternativa escalable,
pero para mensajes breves el método sincronizado es suficiente. Quitar toda
sincronización confiaría en detalles internos de `PrintStream` y dificultaría extender
el registro con más de una instrucción atómica.

---

## 13. Prevención de deadlock, inanición y livelock

Antes de analizar cada mecanismo conviene distinguir los tres problemas:

- **Deadlock:** dos o más hilos quedan bloqueados para siempre porque cada uno espera
  una acción que solo puede realizar otro hilo también bloqueado.
- **Inanición:** el sistema continúa avanzando, pero un hilo particular nunca consigue
  el recurso o turno que necesita.
- **Livelock:** los hilos no están bloqueados y siguen ejecutando acciones, pero
  reaccionan entre sí de tal forma que ninguno completa su trabajo.

### 13.1 Monitores: `synchronized`, `wait` y `notifyAll`

Se usan en `Parque` para la apertura, el cierre y el contador de personas, y en
`Registro` para serializar mensajes.

#### Cómo se evita el deadlock

Los métodos sincronizados de `Parque` realizan operaciones breves y no intentan adquirir
otro lock mientras conservan el monitor. Así se evita formar un ciclo del tipo
“el hilo A posee el monitor del parque y espera el de una atracción, mientras B posee el
de la atracción y espera el del parque”.

Cuando un visitante o el reloj no puede continuar, llama a `wait()`. Esta operación
libera atómicamente el monitor antes de dormir. Por eso quien debe cambiar la condición
puede entrar en `abrir`, `cerrarIngreso`, `salir` o `cerrarActividades`.

También se llama a `notifyAll()` después de cada transición relevante. Si se omitiera la
notificación, los hilos podrían permanecer dormidos aunque la guarda ya fuera verdadera.

#### Cómo se reduce la inanición

`notifyAll()` despierta a todos los posibles interesados, en lugar de elegir
arbitrariamente uno con `notify()`. Esto es importante porque en el mismo monitor puede
haber visitantes esperando la apertura y un reloj esperando que el contador llegue a
cero. Cada hilo vuelve a comprobar su propia condición.

Los monitores intrínsecos de Java no ofrecen una política FIFO. Por lo tanto, no existe
una garantía estricta de equidad, pero las secciones críticas son muy cortas y ningún
hilo conserva el monitor mientras realiza una atracción o duerme, lo que hace improbable
la inanición.

#### Cómo se evita el livelock

Los hilos que no pueden progresar se bloquean con `wait()`; no cambian repetidamente el
estado ni consultan la condición en un bucle activo. Solo reanudan su actividad cuando
existe una notificación y vuelven a evaluar la guarda.

El uso de:

```java
while (!condicion) {
    wait();
}
```

también evita continuar por un despertar espurio. Un `if` podría dejar pasar un hilo
cuando la condición ya volvió a ser falsa.

### 13.2 Semáforos

Se utilizan en los molinetes, la montaña rusa, los autitos, el comedor, el teatro y el
shopping.

#### Cómo se evita el deadlock

Cada permiso adquirido se libera dentro de un bloque `finally`. Esto garantiza la
devolución incluso si ocurre una interrupción o una excepción:

```java
semaforo.acquire();
try {
    usarRecurso();
} finally {
    semaforo.release();
}
```

Además, un visitante no conserva permisos de una atracción mientras intenta adquirir
recursos de otra: completa una actividad y libera sus permisos antes de comenzar la
siguiente. De ese modo no se forma una espera circular entre atracciones.

En montaña rusa, comedor y teatro el permiso sí se conserva mientras se espera una
barrera. Esto es deliberado: el permiso identifica a los integrantes admitidos en el
grupo. El diseño es seguro siempre que pueda reunirse el tamaño requerido o exista una
política de cancelación. La montaña rusa tiene un tiempo máximo; autitos, comedor y
teatro dependen de que llegue una cantidad múltiplo del tamaño del grupo.

#### Cómo se reduce la inanición

Los semáforos se crean con el parámetro de equidad:

```java
new Semaphore(permisos, true)
```

Cuando varios hilos están bloqueados en `acquire()`, se favorece al que lleva más tiempo
esperando. Así se evita que visitantes recién llegados adelanten indefinidamente a uno
antiguo.

`tryAcquire()` no participa de la cola FIFO del mismo modo que un `acquire()` bloqueante.
Por eso, en montaña rusa y comedor un visitante rechazado espera antes de reintentar.
Esto reduce la competencia agresiva, aunque no constituye una garantía estricta de que
ese visitante será el próximo admitido. Una cola bloqueante sería necesaria para una
garantía más fuerte.

#### Cómo se reduce el livelock

Los visitantes que fallan en `tryAcquire()` no reintentan inmediatamente. En
`Visitante` ejecutan `Thread.sleep(20)`, dando tiempo a que quienes poseen los permisos
terminen y los liberen. Esto evita un bucle activo que consuma CPU.

Sin embargo, todos usan el mismo retraso fijo. En una planificación adversa podrían
despertarse y colisionar repetidamente. Un retraso aleatorio o la espera bloqueante
ordenada de una cola eliminaría mejor ese riesgo.

### 13.3 `CyclicBarrier`

Se usa para formar grupos en montaña rusa, autitos, comedor y teatro.

#### Cómo se evita el deadlock

La barrera se configura con el mismo número de participantes que exige la consigna:
cinco en montaña rusa y teatro, veinte en autitos y cuatro en comedor. La acción de
barrera es breve y no adquiere otros recursos; únicamente registra el inicio. Esto evita
que el último hilo mantenga bloqueados a los demás mientras intenta obtener otro lock.

Al ser cíclica, la barrera crea generaciones separadas. Cuando se completa una
generación, todos sus integrantes avanzan y la instancia queda preparada para el grupo
siguiente.

La montaña rusa agrega `await(2, TimeUnit.SECONDS)`. Si no se completa el carro, vence
el plazo y se reinicia la barrera, evitando una espera infinita.

En autitos, comedor y teatro no hay tiempo máximo. Allí la prevención depende de que la
cantidad de visitantes sea suficiente y múltiplo de veinte, cuatro y cinco
respectivamente. Con los cuarenta visitantes de la simulación se cumple. Para cantidades
arbitrarias, la implementación **no elimina completamente** el riesgo de bloqueo de un
grupo final incompleto.

#### Cómo se reduce la inanición

Una vez que un hilo entra en `await()`, ya pertenece a la generación actual y no puede
ser adelantado por miembros de generaciones posteriores. Los semáforos justos que
preceden a las barreras ayudan además a ordenar quién ingresa al grupo.

La barrera no resuelve la inanición previa al ingreso: esa responsabilidad pertenece al
semáforo o a la política de espera de cada atracción.

#### Cómo se evita el livelock

`await()` bloquea al hilo; no le permite retirarse, ceder el paso y volver a intentar
continuamente. Cuando llega el último participante, todos avanzan en una transición
definida. Si la barrera se rompe, los hilos reciben una excepción en lugar de repetir
silenciosamente el encuentro.

En la montaña rusa, reiniciar concurrentemente una barrera rota podría causar
interacciones entre generaciones. Para un sistema más robusto, el reinicio debería
coordinarse bajo un lock o cada viaje debería poseer su propia barrera.

### 13.4 `BlockingQueue`

Se utiliza en los transportes y en juegos de premios.

#### Cómo se evita el deadlock

La cola encapsula su propia exclusión mutua y sus condiciones “no vacía” y “no llena”.
Mientras un hilo está en `put`, `take`, `offer` o `poll`, no necesita adquirir un lock
externo. Esto evita órdenes de adquisición incompatibles entre productores y
consumidores.

Los transportes usan `poll` con plazo en lugar de esperar indefinidamente:

- el conductor despierta periódicamente para observar `funcionando`;
- un viaje incompleto parte al vencer la espera máxima;
- después del cierre, el hilo puede salir cuando la cola queda vacía.

En premios, `take()` sí puede bloquear indefinidamente si nunca llega trabajo. Para
evitarlo durante el cierre, el principal inserta una solicitud centinela `"FIN"` por
cada encargado.

#### Cómo se reduce la inanición

Las instancias son `ArrayBlockingQueue` con equidad habilitada. La cola conserva el orden
FIFO de los elementos y la política justa favorece a los hilos que esperan desde antes
para insertar o extraer.

Cada solicitud se retira una única vez, por lo que dos encargados no pueden procesar al
mismo visitante y una solicitud antigua no debería ser sobrepasada indefinidamente por
solicitudes nuevas.

#### Cómo se evita el livelock

Cuando la cola está vacía, los consumidores quedan bloqueados en `take()` o `poll`; no
consultan repetidamente `isEmpty()`. Cuando está llena, los productores de premios
pueden bloquear en `put` y los transportes informan el rechazo mediante `offer`.

El conductor toma decisiones desde un único hilo. Esto evita una carrera en la que
varios pasajeros intenten simultáneamente decidir si el vehículo está lleno o si ya
debe partir.

### 13.5 `CountDownLatch`

Cada `SolicitudViaje` tiene un latch privado de una cuenta.

#### Cómo se evita el deadlock

El pasajero ejecuta `await()` sin mantener un semáforo, monitor o lock que el conductor
necesite para finalizar el viaje. El conductor puede ejecutar `countDown()` libremente.
Además, el latch recuerda la señal: si el conductor llama a `countDown()` antes de que
el visitante llegue a `await()`, la espera retorna inmediatamente. No existe una
“notificación perdida”.

Cada solicitud tiene su propio latch. Esto impide que un viaje despierte pasajeros de
otro o que un pasajero espere una señal asociada a una generación equivocada.

El posible fallo no está en el latch sino en su protocolo: si el hilo del transporte
terminara por una excepción antes de ejecutar todos los `countDown`, esos pasajeros
quedarían esperando. Una versión más robusta debería liberar en un `finally` todas las
solicitudes ya aceptadas o completar cada una con un estado de cancelación.

#### Cómo se evita la inanición

Todos los pasajeros seleccionados para un viaje reciben exactamente una llamada a
`terminar()`. El latch no elige un ganador ni distribuye un recurso escaso, de modo que
no hay competencia interna que pueda postergar siempre al mismo pasajero.

La posibilidad de no ser elegido para un viaje se controla antes, mediante el orden FIFO
de la cola.

#### Cómo se evita el livelock

`await()` es una espera pasiva y `countDown()` es irreversible: la cuenta pasa de uno a
cero y nunca vuelve atrás. Los participantes no pueden deshacer mutuamente su progreso
ni entrar en un ciclo de cesiones.

### 13.6 `Exchanger`

Cada solicitud de premio posee un `Exchanger` privado.

#### Cómo se evita el deadlock

El emparejamiento está correlacionado por solicitud. La cola entrega una
`SolicitudPremio` concreta a un encargado, y solo ese encargado y su visitante acceden
al intercambiador. Así se evita que dos visitantes se encuentren entre sí o que un
encargado espere a una persona distinta.

El protocolo es simétrico y tiene un orden fijo:

1. ambos realizan el intercambio de la ficha;
2. ambos realizan el intercambio del premio.

Ninguno conserva locks externos durante `exchange()`, por lo que el encuentro no forma
parte de una cadena circular de locks.

El diseño supone que, después de aceptar una solicitud, visitante y encargado no son
cancelados. Si uno se interrumpe entre los dos encuentros, el otro podría quedar
bloqueado. Una implementación tolerante a fallos usaría `exchange` temporizado y un
estado explícito de cancelación.

#### Cómo se evita la inanición

El intercambiador privado tiene exactamente dos participantes conocidos. No existe un
tercer hilo que pueda adelantarse y apropiarse del intercambio. La equidad entre
solicitudes se resuelve previamente mediante la cola bloqueante.

#### Cómo se evita el livelock

`exchange()` bloquea hasta el encuentro y realiza una transferencia atómica. Los hilos
no intentan entregar, retiran el objeto y vuelven a intentar; cada encuentro constituye
un avance irreversible del protocolo.

### 13.7 `ReentrantLock` y `Condition`

Se utilizan en realidad virtual para reservar un visor, dos manoplas y una base.

#### Cómo se evita el deadlock

Existe un único lock que protege los tres inventarios. Todos los hilos lo adquieren en
el mismo punto y lo liberan en `finally`, por lo que no puede existir un orden inverso
entre varios locks.

La comprobación y la reserva son atómicas. Si falta algún componente, el visitante no
retiene ninguno: llama a `await()`, que libera el lock mientras espera. Esto elimina
“retener y esperar”, una de las condiciones necesarias del deadlock.

Al devolver el equipo, los tres inventarios se actualizan dentro de la misma sección
crítica y después se hace `signalAll()`.

#### Cómo se reduce la inanición

`new ReentrantLock(true)` habilita una política justa de adquisición. Los hilos que
llevan más tiempo esperando tienen preferencia para recuperar el lock.

Se usa `signalAll()` porque despertar solamente un hilo podría elegir uno que no logre
progresar si en el futuro existieran solicitudes de equipos diferentes. Todos vuelven a
evaluar la guarda y solo quienes encuentren un equipo completo descuentan recursos.

La justicia del lock no implica una garantía absoluta de que todos obtendrán equipo
bajo cualquier planificación, pero junto con secciones críticas cortas y devolución en
`finally` reduce fuertemente la inanición.

#### Cómo se evita el livelock

Los visitantes sin equipo completo duermen en `await()`; no toman componentes parciales,
los devuelven y vuelven a competir. Cuando despiertan, verifican la condición dentro del
lock, de modo que solo uno puede confirmar y modificar un inventario determinado a la
vez.

### 13.8 `volatile`

Se utiliza en `funcionando` para el barco y el tren, y en `abierto` para premios.

#### Relación con deadlock, inanición y livelock

`volatile` no bloquea y, por lo tanto, por sí mismo no puede crear un deadlock. Su
función es garantizar que un hilo observe el cambio realizado por otro sin mantener una
copia desactualizada.

En los transportes evita que el conductor siga ejecutando para siempre por no ver
`funcionando = false`. El `poll` temporizado garantiza que despierte periódicamente y
pueda consultar la bandera; la combinación evita una espera eterna durante el cierre.

`volatile` no hace atómicas operaciones compuestas y no sustituye un lock. Sería
incorrecto usarlo para proteger los contadores de realidad virtual o
`personasDentro++/--`.

La variable `abierto` de premios actualmente se escribe y puede consultarse, pero el
protocolo efectivo de cierre se basa en los centinelas. Por sí sola no despertaría a un
encargado bloqueado en `take()`.

### 13.9 `join`

El hilo principal usa `join()` para esperar visitantes, transportes, encargados y reloj.

#### Cómo se evita el deadlock

El principal no conserva locks ni permisos mientras ejecuta `join()`. Los hilos
esperados pueden continuar y liberar todos sus recursos. Además, la secuencia de cierre
respeta las dependencias:

1. espera a los visitantes;
2. solicita el cierre de transportes y premios;
3. espera a empleados y conductores;
4. espera al reloj.

No se hace que un trabajador espere al principal mientras el principal espera a ese
trabajador.

#### Cómo se evita la inanición y el livelock

`join()` no compite por turnos: espera pasivamente un evento único, la terminación del
hilo. No realiza sondeos ni modifica decisiones de otros hilos, por lo que no introduce
livelock.

Sin embargo, propaga los bloqueos existentes: si un grupo queda incompleto en una
barrera, ese visitante no termina y el principal tampoco retorna de `join()`. Por eso la
correcta política de cierre de cada atracción es necesaria para garantizar la
finalización global.

### 13.10 Evaluación por atracción

| Área | Deadlock | Inanición | Livelock |
|---|---|---|---|
| Parque y molinetes | `wait` libera el monitor; permisos en `finally` | Semáforo justo y secciones breves | Esperas bloqueantes, sin sondeo activo |
| Montaña rusa | Reserva acotada y barrera con tiempo máximo | Semáforos justos; el reintento no tiene garantía FIFO estricta | Pausa entre reintentos; sería mejor un retraso aleatorio |
| Autitos | Permisos en `finally`; requiere grupos completos | Semáforo justo | `await` bloqueante, sin cesiones repetidas |
| Barco y tren | Cola sin locks externos, `poll` temporizado, latch privado | Cola FIFO justa | Un solo conductor decide la partida |
| Premios | Cola correlaciona y `Exchanger` es privado | Cola justa; dos participantes por canje | Intercambios atómicos, sin reintento activo |
| Comedor | Liberación en `finally`; requiere múltiplos de cuatro | Semáforo justo, aunque `tryAcquire` no reserva turno | Pausa antes de reintentar |
| Teatro | Liberación en `finally`; requiere múltiplos de cinco | Semáforo justo | Barrera bloqueante |
| Realidad virtual | Reserva todo-o-nada bajo un único lock | Lock justo y `signalAll` | No se toman y devuelven recursos parcialmente |
| Shopping | Un único semáforo liberado en `finally` | Semáforo justo | Espera bloqueante en `acquire` |

> [!important] Garantía y supuesto no son lo mismo
> Los mecanismos eliminan ciclos de locks y esperas activas en el recorrido normal,
> pero autitos, comedor y teatro aún dependen del supuesto de grupos completos. Por eso
> no sería correcto afirmar que el programa garantiza ausencia absoluta de bloqueos
> para cualquier cantidad de visitantes o ante cualquier interrupción.

---

## 14. Cierre ordenado

La finalización usa mecanismos distintos según el tipo de hilo:

- los visitantes terminan naturalmente y el principal los espera con `join`;
- premios recibe un centinela `"FIN"` por encargado;
- barco y tren cambian una bandera `volatile` a `false` y continúan mientras su cola no
  esté vacía;
- el reloj espera mediante el monitor hasta que `personasDentro == 0`.

`volatile` es suficiente para `funcionando` porque solo se necesita visibilidad de una
asignación simple; el contenido de la cola ya está protegido por `BlockingQueue`.

El hilo de transporte usa `poll` con plazo y no `take`, porque con `take()` podría quedar
bloqueado para siempre después del cierre si la cola está vacía. Otra solución sería
encolar un centinela, como en premios.

---

## 15. Correspondencia resumida entre consignas y mecanismos

| Consigna | Mecanismo | Propiedad garantizada |
|---|---|---|
| `k` molinetes | `Semaphore(k)` | A lo sumo `k` ingresos simultáneos |
| Abrir, cerrar y esperar parque vacío | Monitor | Estado horario coherente y espera por guardas |
| Montaña rusa | 2 semáforos + barrera de 5 | Espera limitada, cinco asientos y salida completa |
| Autitos | Semáforo + barrera de 20 | Pista exclusiva por turno y veinte ocupantes |
| Barco | `BlockingQueue` + latch por pasajero | Fila, lote por capacidad/tiempo y espera del final |
| Premios | `BlockingQueue` + `Exchanger` privado | Asignación a empleado y canje correlacionado |
| Comedor | Semáforo + barrera de 4 | Capacidad total y comienzo en grupos completos |
| Tren | `BlockingQueue` + latch por pasajero | Salida con diez o por tiempo y fin individual |
| Teatro | Semáforo + barrera de 5 | Capacidad veinte e ingreso de a cinco |
| Realidad virtual | `ReentrantLock` + `Condition` | Reserva atómica de un equipo compuesto |
| Shopping | Semáforo | Capacidad máxima |
| Salida del programa | `join`, centinelas y bandera `volatile` | No abandonar hilos activos |

## 16. Límites que conviene reconocer en una defensa

La solución demuestra los mecanismos pedidos, pero una explicación rigurosa debe
separar el objetivo de las garantías efectivas:

1. el cierre de actividades se registra, pero todavía no se aplica antes de cada
   atracción;
2. teatro no modela funciones periódicas, solo capacidad y grupos;
3. barreras sin una política general de cancelación pueden dejar grupos incompletos;
4. el comedor forma mesas lógicas globales, no objetos mesa independientes;
5. si se llena la fila del barco, el visitante actual no reintenta;
6. la hora 23:00 espera que todos salgan, pero no fuerza la evacuación;
7. usar cuarenta visitantes evita restos en grupos de cuatro, cinco y veinte, pero esa
   elección de datos no reemplaza una política de cierre para cantidades arbitrarias.

Estas observaciones no invalidan las elecciones principales. Indican qué capas deberían
añadirse para transformar la simulación académica en un sistema general y resistente a
interrupciones, cierres y cantidades arbitrarias.
