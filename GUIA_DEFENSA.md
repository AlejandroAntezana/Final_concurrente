# Guía de defensa — Parque de Diversiones Concurrente

> [!info] Cómo usar esta guía
> Intentá responder cada pregunta en voz alta antes de desplegar la respuesta.
> En Obsidian, las respuestas aparecen plegadas porque usan callouts con `-`.
> Las preguntas marcadas con **⚠ Pregunta crítica** señalan límites reales del código: conviene reconocerlos con precisión y explicar cómo se mejorarían.

## Índice

- [[#1. Visión general y arquitectura]]
- [[#2. Conceptos fundamentales de concurrencia]]
- [[#3. Programa principal, ciclo de vida y registro]]
- [[#4. Parque, horarios, molinetes y salida]]
- [[#5. Montaña rusa]]
- [[#6. Autitos chocadores]]
- [[#7. Barco pirata y tren]]
- [[#8. Juegos de premios]]
- [[#9. Comedor]]
- [[#10. Teatro]]
- [[#11. Realidad virtual]]
- [[#12. Shopping]]
- [[#13. Seguridad, progreso e interrupciones]]
- [[#14. Preguntas sobre el enunciado y cobertura]]
- [[#15. Preguntas críticas y mejoras]]
- [[#16. Recorrido rápido para una exposición oral]]

---

## 1. Visión general y arquitectura

### 1. ¿Qué problema resuelve el trabajo?

> [!answer]- Respuesta
> Simula un parque de diversiones donde visitantes, empleados, transportes y reloj avanzan concurrentemente. Cada atracción impone restricciones diferentes: capacidad, formación de grupos, filas, intercambio de objetos, disponibilidad de recursos compuestos y horarios. El objetivo central no es solo representar actividades, sino coordinarlas sin carreras y usando los mecanismos exigidos por la consigna.

### 2. ¿Cuál es la arquitectura general?

> [!answer]- Respuesta
> Se usa un hilo por objeto activo: cada `Visitante`, el `RelojParque`, el encargado del barco, el conductor del tren y cada `EncargadoPremios`. Las atracciones son objetos pasivos compartidos que encapsulan sus propios mecanismos de sincronización. `SimulacionParque` construye los recursos, crea e inicia los hilos y coordina su finalización.

### 3. ¿Por qué las atracciones no son todas hilos?

> [!answer]- Respuesta
> Una atracción solo necesita hilo propio cuando tiene comportamiento autónomo, por ejemplo formar viajes aunque no esté siendo invocada directamente. Por eso barco y tren implementan `Runnable`. En montaña rusa, comedor, teatro, VR y shopping, la operación ocurre como parte del hilo visitante y el objeto solo protege estado compartido. Crear un hilo por cada recurso sin comportamiento autónomo agregaría complejidad sin aportar concurrencia útil.

### 4. ¿Qué objetos son compartidos?

> [!answer]- Respuesta
> Cada instancia de `Parque`, `MontanaRusa`, `AutitosChocadores`, `TransporteConCola`, `JuegosDePremios`, `Comedor`, `Teatro`, `RealidadVirtual` y `Shopping` se comparte entre los 40 visitantes. Esa compartición es la razón por la que sus estados deben protegerse. `Ficha`, `Premio`, `SolicitudViaje` y `SolicitudPremio` representan datos o coordinaciones particulares.

### 5. ¿Por qué se eligieron 40 visitantes?

> [!answer]- Respuesta
> Porque 40 es múltiplo de 4, 5, 10 y 20. Así se completan mesas, carros, grupos de teatro, trenes y turnos de autitos en esta simulación controlada. Es una precondición práctica del escenario actual, no una solución general al problema de grupos incompletos.

### 6. ¿El recorrido de todos los visitantes es aleatorio?

> [!answer]- Respuesta
> No. Todos siguen el mismo orden de atracciones definido en `Visitante.run()`: shopping, montaña rusa, autitos, barco, premios, comedor, tren, teatro y realidad virtual. Lo aleatorio es el puntaje de premios mediante `Random.nextInt(101)`. El entrelazado sí cambia entre ejecuciones por la planificación de los hilos.

### 7. ¿Qué significa encapsular la sincronización?

> [!answer]- Respuesta
> Significa que cada recurso garantiza internamente sus invariantes. El visitante pide `subir`, `viajar`, `comer` o `usar` sin manipular permisos, locks o colas directamente. Esto reduce el acoplamiento y evita que una clase externa pueda olvidar liberar un recurso o modificar contadores sin protección.

---

## 2. Conceptos fundamentales de concurrencia

### 8. ¿Qué diferencia hay entre concurrencia y paralelismo?

> [!answer]- Respuesta
> Concurrencia significa que varias tareas progresan de forma solapada y sus operaciones pueden intercalarse. Paralelismo significa que realmente ejecutan al mismo tiempo en distintos núcleos. El programa necesita ser correcto bajo cualquier intercalado, incluso en una máquina de un solo núcleo.

### 9. ¿Qué es una condición de carrera?

> [!answer]- Respuesta
> Es un error donde el resultado depende de un intercalado no controlado sobre estado compartido. Por ejemplo, dos incrementos concurrentes de `personasDentro` podrían perderse porque `++` es lectura, suma y escritura, no una operación atómica. En `Parque` se evita modificando el contador dentro del monitor.

### 10. ¿Qué propiedades se buscan en una solución concurrente?

> [!answer]- Respuesta
> Seguridad: nunca ocurre un estado inválido, como superar una capacidad o entregar un equipo incompleto. Progreso: los hilos que pueden avanzar eventualmente avanzan. También se busca visibilidad de memoria, exclusión mutua donde corresponde y finalización ordenada.

### 11. ¿Cuál es la diferencia entre exclusión mutua y sincronización por condición?

> [!answer]- Respuesta
> La exclusión mutua evita que dos hilos ejecuten simultáneamente una sección crítica. La sincronización por condición hace que un hilo espere hasta que cierto estado sea válido. Un lock puede proteger los inventarios de VR; la `Condition` permite además esperar hasta disponer de un equipo completo.

### 12. ¿Qué es una relación *happens-before* relevante aquí?

> [!answer]- Respuesta
> Es una garantía de visibilidad y orden definida por el modelo de memoria de Java. Liberar y luego adquirir el mismo monitor o lock, insertar y extraer mediante una colección concurrente, escribir y leer una variable `volatile`, iniciar un hilo con `start()` y completar un `join()` establecen relaciones que hacen visibles las escrituras previas.

### 13. ¿Por qué `Thread.sleep()` no sincroniza?

> [!answer]- Respuesta
> Porque solo suspende aproximadamente al hilo durante un tiempo; no libera monitores que posea, no espera una condición lógica y no garantiza que otro hilo haya terminado. En este proyecto se usa para simular duración, mientras que `wait`, `await`, barreras, latches y `join` realizan la coordinación real.

### 14. ¿Qué diferencia hay entre espera bloqueante y espera activa?

> [!answer]- Respuesta
> En una espera bloqueante el hilo queda suspendido y no consume CPU hasta recibir una señal o cumplirse un plazo. En espera activa consulta repetidamente una condición. El proyecto usa esperas bloqueantes en colas, barreras, condiciones, latches y monitores. Los reintentos de algunas atracciones llevan un `sleep(20)` para no girar continuamente, aunque una cola bloqueante sería una solución más fuerte.

### 15. ¿Por qué se usa `while` y no `if` alrededor de `wait()` o `await()`?

> [!answer]- Respuesta
> Porque puede haber despertares espurios y porque, al recuperar el lock, otro hilo pudo haber cambiado nuevamente el estado. El patrón correcto es comprobar la guarda en un `while`, dormir liberando el lock y volver a comprobarla al despertar.

### 16. ¿Qué aporta la equidad configurada con `true`?

> [!answer]- Respuesta
> En `Semaphore`, `ArrayBlockingQueue` y `ReentrantLock`, la política justa favorece aproximadamente el orden de espera y reduce el riesgo de inanición. Tiene costo de rendimiento y no constituye una garantía absoluta de planificación FIFO en todas las circunstancias.

---

## 3. Programa principal, ciclo de vida y registro

### 17. ¿Qué responsabilidades tiene `SimulacionParque`?

> [!answer]- Respuesta
> Configura capacidades y tiempos, crea una única instancia de cada recurso compartido, construye los hilos, los inicia en un orden controlado, espera a los visitantes, ordena el cierre de servicios y finalmente hace `join` de todos. También asigna nombres descriptivos que aparecen en las trazas.

### 18. ¿Qué diferencia existe entre invocar `start()` e invocar `run()`?

> [!answer]- Respuesta
> `start()` crea la ejecución concurrente y la JVM invoca `run()` en el nuevo hilo. Llamar `run()` directamente sería una llamada normal ejecutada por el hilo principal y eliminaría la concurrencia.

### 19. ¿Por qué se hace `join()`?

> [!answer]- Respuesta
> Para esperar la terminación real de otro hilo. Primero el principal espera a todos los visitantes; recién entonces cierra transportes y premios. Después espera a empleados, transportes y reloj. Esto evita terminar la coordinación mientras aún hay trabajo activo y aporta visibilidad de todo lo hecho por el hilo finalizado.

### 20. ¿Por qué no reemplazar `join()` por un `sleep()` largo?

> [!answer]- Respuesta
> Porque un tiempo estimado puede ser insuficiente o excesivo y no demuestra que el hilo haya terminado. `join()` espera exactamente el evento requerido, independientemente de la velocidad del equipo o del planificador.

### 21. ¿Cómo se muestran los distintos hilos en terminal?

> [!answer]- Respuesta
> Todos llaman a `Registro.informar`. El método es `static synchronized` y antepone milisegundos transcurridos y `Thread.currentThread().getName()`. Así cada línea identifica qué hilo produjo el evento y las impresiones propias del registro no se mezclan.

### 22. ¿El orden de los mensajes será siempre igual?

> [!answer]- Respuesta
> No. El registro serializa la escritura de cada mensaje, pero no impone un orden global a eventos independientes. El planificador puede intercalar los hilos de formas distintas. Esa no determinación es normal y permite observar la concurrencia.

### 23. ¿El método sincronizado de registro puede afectar la simulación?

> [!answer]- Respuesta
> Introduce una sección crítica global muy corta, por lo que puede alterar levemente el *timing*, pero no las reglas funcionales. Para una simulación académica es suficiente. Una alternativa más escalable sería una `BlockingQueue` de eventos consumida por un único hilo registrador.

---

## 4. Parque, horarios, molinetes y salida

### 24. ¿Cómo se modelan los `k` molinetes?

> [!answer]- Respuesta
> Con un `Semaphore(k, true)`. Cada permiso representa un molinete disponible. El visitante hace `acquire`, simula el cruce y libera en `finally`. Esto garantiza como máximo `k` ingresos simultáneos sin obligar a que lleguen en grupos de `k`.

### 25. ¿Por qué una barrera no serviría para los molinetes?

> [!answer]- Respuesta
> Una barrera de `k` obligaría a esperar hasta reunir exactamente `k` visitantes antes de dejarlos avanzar. Los molinetes representan capacidad simultánea: también deben poder entrar una o dos personas si hay lugares libres.

### 26. ¿Por qué `Parque` funciona como monitor?

> [!answer]- Respuesta
> Sus métodos `synchronized` y bloques `synchronized(this)` protegen conjuntamente `ingresoAbierto`, `ingresoFinalizado`, `actividadesAbiertas` y `personasDentro`. Los visitantes esperan la apertura y el reloj espera que el contador llegue a cero usando `wait/notifyAll` bajo el mismo monitor.

### 27. ¿Por qué hay dos variables para el ingreso: abierto y finalizado?

> [!answer]- Respuesta
> Permiten distinguir “todavía no abrió” de “ya cerró definitivamente”. Antes de las 09:00 el visitante espera; después de las 18:00 debe retornar `false` y no esperar una reapertura que no ocurrirá.

### 28. ¿Por qué se usa `notifyAll()` en vez de `notify()`?

> [!answer]- Respuesta
> En el mismo monitor pueden esperar condiciones lógicas distintas: visitantes por la apertura y el reloj por parque vacío. `notify()` podría despertar un hilo que no puede progresar y dejar dormido al adecuado. `notifyAll()` despierta a todos para que cada uno reevalúe su guarda.

### 29. ¿Cómo se garantiza que a las 23:00 no quede nadie?

> [!answer]- Respuesta
> El reloj llama a `esperarParqueVacio()`, que espera mientras `personasDentro > 0`. Cada visitante decrementa el contador al salir y notifica. Solo después de observar cero se informa el cierre de las 23:00. Es una espera por vaciado, no una expulsión forzada a una hora límite real.

### 30. **⚠ Pregunta crítica:** ¿el cierre de actividades a las 19:00 se hace cumplir?

> [!answer]- Respuesta
> No completamente. `cerrarActividades()` cambia la bandera e informa el evento, pero `Visitante.run()` no consulta `estanAbiertasLasActividades()` antes de cada atracción. Por lo tanto, el código anuncia el cierre pero un visitante ya dentro puede seguir iniciando actividades. Lo corregiría verificando la bandera antes de cada actividad y haciendo que cada recurso rechace nuevas entradas al cerrar, sin abandonar usuarios que ya comenzaron.

### 31. ¿Qué ocurre si un visitante es interrumpido después de entrar?

> [!answer]- Respuesta
> `Visitante.run()` restaura el indicador de interrupción y el bloque `finally` llama a `parque.salir(nombre)` si había ingresado. Así no queda contabilizado dentro del parque. Además, los recursos que adquieren permisos o equipos suelen liberarlos en `finally`.

---

## 5. Montaña rusa

### 32. ¿Cómo se satisfacen todos los requisitos de la montaña rusa?

> [!answer]- Respuesta
> `lugarDeEspera` limita la zona de espera y usa `tryAcquire` para que quien la encuentre llena se retire. `asientos`, un semáforo de cinco, evita más de cinco ocupantes y se conserva durante el viaje. `barreraSalida`, una `CyclicBarrier(5)`, impide comenzar hasta llenar el carro.

### 33. ¿Por qué hacen falta semáforo y barrera?

> [!answer]- Respuesta
> Resuelven propiedades distintas. El semáforo expresa “como máximo cinco”; la barrera expresa “no continuar hasta que sean cinco”. Usar solo uno dejaría sin garantizar la otra propiedad.

### 34. ¿Por qué la barrera es cíclica?

> [!answer]- Respuesta
> Porque la coordinación se repite para múltiples carros. Al liberar un grupo de cinco, `CyclicBarrier` comienza automáticamente una nueva generación. Un `CountDownLatch` no puede reiniciarse.

### 35. ¿Qué ocurre si no se reúnen cinco pasajeros?

> [!answer]- Respuesta
> `await(2, TimeUnit.SECONDS)` vence, se reinicia la barrera y el método retorna `false`; el visitante puede reintentar. Esto evita una espera indefinida en ese caso, aunque el `reset()` concurrente puede volver delicada la separación entre generaciones y una versión de producción debería administrar cada viaje bajo un lock.

### 36. ¿Cómo se evita que suba el siguiente grupo antes de terminar el viaje actual?

> [!answer]- Respuesta
> Los cinco pasajeros conservan los permisos de `asientos` durante el `sleep` que simula el viaje y solo los liberan en `finally`. El siguiente grupo no puede adquirir asientos hasta entonces.

---

## 6. Autitos chocadores

### 37. ¿Cómo se representan diez autos con dos personas cada uno?

> [!answer]- Respuesta
> Se modela la capacidad total: un `Semaphore(20, true)` limita la pista y una `CyclicBarrier(20)` exige que estén presentes las veinte personas antes de empezar. La acción de la barrera anuncia que los diez autos están completos.

### 38. ¿El código asigna explícitamente parejas a cada auto?

> [!answer]- Respuesta
> No. Garantiza veinte ocupantes totales, lo cual permite interpretar diez autos completos, pero no crea diez objetos `Auto` ni registra parejas. Si se necesitara identidad por auto, usaría diez coordinaciones de dos personas y una segunda coordinación global para iniciar el turno.

### 39. **⚠ Pregunta crítica:** ¿puede quedar bloqueado un turno de autitos?

> [!answer]- Respuesta
> Sí, si el total que llega no permite completar veinte o si una interrupción rompe la generación. En el escenario dado llegan 40 visitantes, por eso se forman dos turnos exactos. Para hacerlo general agregaría tiempo máximo, cancelación coherente y una política de cierre para liberar o rechazar el grupo incompleto.

---

## 7. Barco pirata y tren

### 40. ¿Por qué barco y tren comparten `TransporteConCola`?

> [!answer]- Respuesta
> Ambos tienen el mismo patrón: fila limitada, capacidad máxima, salida al llenarse o al vencer un plazo y notificación individual al terminar. Se parametrizan nombre, capacidad de viaje, capacidad de fila y espera máxima, evitando duplicación.

### 41. ¿Cómo funciona el patrón productor–consumidor?

> [!answer]- Respuesta
> Cada visitante produce una `SolicitudViaje` y la ofrece a una `ArrayBlockingQueue`. El hilo encargado consume la primera con `poll`, completa un arreglo hasta capacidad o plazo, realiza el viaje y finaliza las solicitudes. La cola protege internamente sus índices y coordina productores y consumidor.

### 42. ¿Por qué se usa `offer()` al ingresar a la fila?

> [!answer]- Respuesta
> Porque retorna inmediatamente `false` si la fila está llena, permitiendo modelar el rechazo o reintento sin bloquear al visitante indefinidamente. `put()` lo dejaría esperando hasta que aparezca espacio.

### 43. ¿Cómo se implementa “sale lleno o por tiempo”?

> [!answer]- Respuesta
> El encargado fija un límite cuando obtiene al primer pasajero. Luego ejecuta `poll(restante, TimeUnit.MILLISECONDS)` hasta alcanzar la capacidad o agotar el tiempo. La decisión queda en un solo hilo, reduciendo carreras entre “se llenó” y “venció el plazo”.

### 44. ¿Por qué cada solicitud tiene un `CountDownLatch(1)`?

> [!answer]- Respuesta
> Cada visitante debe esperar un único evento: que termine su viaje específico. El encargado llama `countDown()` y el visitante espera con `await()`. La señal no se pierde aunque ocurra antes del `await`, y un latch individual no despierta pasajeros de otro viaje.

### 45. ¿Por qué no usar una `CyclicBarrier` para barco o tren?

> [!answer]- Respuesta
> Porque deben poder salir con un grupo incompleto cuando vence el tiempo. Una barrera fija espera a todos sus participantes y no representa naturalmente la selección de un lote ordenado desde una fila.

### 46. ¿Cómo finalizan los hilos de transporte?

> [!answer]- Respuesta
> `cerrar()` escribe `funcionando = false`. El `run` continúa mientras el servicio funcione o la cola no esté vacía, por lo que procesa pedidos pendientes. Usa `poll` con plazo y no `take`, evitando quedar bloqueado para siempre cuando ya cerró y la cola está vacía.

### 47. ¿Por qué `funcionando` es `volatile`?

> [!answer]- Respuesta
> El principal escribe la bandera y el hilo de transporte la lee sin compartir un lock. `volatile` garantiza visibilidad de esa asignación. Es suficiente porque solo se requiere leer/escribir un booleano; no se intenta convertir en atómica una operación compuesta.

### 48. **⚠ Pregunta crítica:** ¿qué pasa si se llena la fila del barco?

> [!answer]- Respuesta
> `viajar` retorna `false`, pero el recorrido actual no reintenta el barco, a diferencia del tren. Por eso el caso de fila completamente llena no cumple literalmente “esperar al próximo viaje”. Con 40 lugares de fila y 40 visitantes es poco probable en esta configuración, pero la solución robusta sería reintentar mientras las actividades estén abiertas o usar una espera bloqueante cancelable.

---

## 8. Juegos de premios

### 49. ¿Cómo se coordinan visitantes y encargados?

> [!answer]- Respuesta
> El visitante encola una `SolicitudPremio`. Uno de los dos encargados la toma. En un primer encuentro mediante `Exchanger`, el visitante entrega la `Ficha`; el encargado calcula el premio. En un segundo encuentro entrega el `Premio` y el visitante recibe el resultado.

### 50. ¿Por qué hay un `Exchanger` por solicitud?

> [!answer]- Respuesta
> Porque `Exchanger` empareja a dos hilos, pero no conoce identidades. Si todos usaran uno global, podrían encontrarse dos visitantes o dos encargados e intercambiar objetos equivocados. El intercambiador privado correlaciona exactamente un visitante con el encargado que tomó su solicitud.

### 51. ¿Qué función cumple la `BlockingQueue` si ya existe el `Exchanger`?

> [!answer]- Respuesta
> La cola asigna cada solicitud a un encargado y mantiene el orden/contrapresión. El `Exchanger` sincroniza y transfiere objetos entre los dos participantes ya asociados. Son responsabilidades distintas.

### 52. ¿Cómo se decide el tamaño del premio?

> [!answer]- Respuesta
> El puntaje aleatorio está entre 0 y 100 inclusive. Con 80 o más se entrega premio grande, con 40 a 79 mediano y con menos de 40 pequeño. La decisión la toma el encargado después de recibir la ficha.

### 53. ¿Cómo se cierran los encargados que esperan en `take()`?

> [!answer]- Respuesta
> El principal inserta una solicitud centinela con visitante `"FIN"` por cada encargado. Cada uno eventualmente toma una, sale de su bucle y termina. Debe haber tantos centinelas como consumidores; uno solo dejaría al otro bloqueado.

### 54. ¿La variable `abierto` es necesaria actualmente?

> [!answer]- Respuesta
> Se marca `volatile` y cambia al cerrar, pero el recorrido actual no consulta `estaAbierto()` antes de jugar. Por eso hoy es redundante para el control efectivo. Podría usarse para rechazar solicitudes tardías o eliminarse si el cierre sigue dependiendo exclusivamente de centinelas.

---

## 9. Comedor

### 55. ¿Cómo se modela el comedor?

> [!answer]- Respuesta
> Un semáforo con `cantidadMesas * 4` permisos limita la capacidad total; con tres mesas son doce personas. Una `CyclicBarrier(4)` forma lotes de cuatro y recién al llegar el cuarto todos comienzan a comer.

### 56. ¿Qué ocurre si el comedor está lleno?

> [!answer]- Respuesta
> `tryAcquire()` retorna `false`, el visitante informa que volverá luego y `Visitante` reintenta después de una pausa breve. Esto implementa la opción del enunciado de esperar o retirarse temporalmente.

### 57. ¿Cada barrera representa una mesa física?

> [!answer]- Respuesta
> No. Es una barrera global reutilizable que forma mesas lógicas consecutivas de cuatro. Satisface el comienzo grupal en el escenario uniforme, pero no identifica tres mesas independientes. Un modelo más fiel tendría objetos `Mesa`, cada uno con su capacidad, generación y estado de ocupación.

### 58. **⚠ Pregunta crítica:** ¿qué pasa con un último grupo de menos de cuatro?

> [!answer]- Respuesta
> Queda bloqueado porque la barrera no tiene tiempo máximo. Los 40 visitantes evitan ese resto. Una solución general necesita política de cierre, `await` temporizado o un coordinador que cancele el grupo incompleto de forma consistente.

---

## 10. Teatro

### 59. ¿Cómo se modela la capacidad y el ingreso grupal?

> [!answer]- Respuesta
> Un `Semaphore(20, true)` impide superar la capacidad de la sala y una `CyclicBarrier(5)` obliga a ingresar en grupos completos de cinco. Los permisos se conservan durante la simulación del espectáculo y se liberan en `finally`.

### 60. ¿Por qué no basta un semáforo de veinte?

> [!answer]- Respuesta
> Solo garantizaría capacidad máxima; permitiría que las personas ingresaran individualmente. La barrera agrega la condición de encuentro de cinco.

### 61. **⚠ Pregunta crítica:** ¿se modela que el espectáculo ocurre periódicamente?

> [!answer]- Respuesta
> No. Se modelan capacidad, grupos y duración, pero no hay un hilo de funciones ni una compuerta asociada a horarios de espectáculo. Para cubrirlo completamente agregaría un coordinador del teatro que abra cada función, admita hasta cuatro grupos y cierre el ingreso cuando comienza.

---

## 11. Realidad virtual

### 62. ¿Cuál es la condición para ingresar a VR?

> [!answer]- Respuesta
> Deben estar disponibles simultáneamente un visor, dos manoplas y una base. Los tres contadores se comprueban y actualizan bajo el mismo `ReentrantLock`, de modo que la reserva del equipo es atómica.

### 63. ¿Por qué se eligió `ReentrantLock` con `Condition`?

> [!answer]- Respuesta
> La disponibilidad es una guarda sobre varios contadores relacionados. `Condition.await()` permite dormir liberando el lock hasta que una devolución pueda hacer cierta la guarda. Además, el lock justo ayuda a reducir inanición y cumple el requisito del enunciado de usar locks explícitos.

### 64. ¿Cómo se evita entregar un equipo incompleto?

> [!answer]- Respuesta
> La toma es todo-o-nada: primero se verifica la guarda completa y luego se descuentan todos los componentes dentro de la misma sección crítica. Ningún otro hilo puede intercalarse entre la verificación y la reserva.

### 65. ¿Cómo se evita un deadlock por recursos parciales?

> [!answer]- Respuesta
> Nadie retiene un visor mientras espera manoplas o una base. Si falta cualquier componente, el visitante espera sin tomar ninguno. Esto elimina la condición de “retener y esperar”, necesaria para formar una espera circular.

### 66. ¿Por qué no usar tres semáforos independientes?

> [!answer]- Respuesta
> Una adquisición secuencial podría dejar recursos retenidos parcialmente: varios hilos toman visores y esperan manoplas, por ejemplo. Se podría diseñar devolución ante fallo, pero sería más complejo y propenso a inanición. El lock único permite evaluar y reservar el conjunto atómicamente.

### 67. ¿Por qué se llama `signalAll()` al devolver?

> [!answer]- Respuesta
> Una devolución modifica varios contadores y puede habilitar a más de un visitante. Como no se sabe qué hilo podrá satisfacer la guarda cuando recupere el lock, se despiertan todos y cada uno la reevalúa. Solo los que consigan un equipo completo continúan.

### 68. ¿Se pierden equipos si el visitante es interrumpido durante el uso?

> [!answer]- Respuesta
> No. Después de tomar el equipo, `usar()` ejecuta la devolución en un `finally`. El lock también se libera siempre en `finally`. Así una interrupción durante el `sleep` no deja componentes descontados.

---

## 12. Shopping

### 69. ¿Qué sincronización tiene el shopping?

> [!answer]- Respuesta
> Un semáforo justo limita su ocupación a quince visitantes. Cada visitante adquiere un permiso, recorre el lugar y lo libera en `finally`. No necesita barrera porque no exige formar grupos.

### 70. ¿El visitante realmente “opta” entre shopping y actividades?

> [!answer]- Respuesta
> No en el sentido de una elección excluyente: el recorrido actual hace que todos visiten el shopping y luego todas las actividades. Para modelar opciones, `Visitante` debería seleccionar aleatoriamente o mediante una estrategia su próxima actividad, respetando horarios y evitando imponer un itinerario único.

---

## 13. Seguridad, progreso e interrupciones

### 71. Definí deadlock, inanición y livelock.

> [!answer]- Respuesta
> **Deadlock:** un conjunto de hilos queda bloqueado para siempre por una espera circular. **Inanición:** el sistema progresa, pero un hilo particular nunca obtiene el recurso. **Livelock:** los hilos siguen ejecutando y reaccionando entre sí, pero no completan trabajo útil.

### 72. ¿Qué decisiones reducen el deadlock?

> [!answer]- Respuesta
> No se mantienen locks mientras se espera otro recurso; `wait/await` liberan el lock; VR reserva todo o nada; permisos y locks se liberan en `finally`; los latches son particulares; las colas encapsulan su sincronización; el principal hace `join` sin poseer recursos. Aun así, barreras con grupos incompletos son un riesgo reconocido.

### 73. ¿Qué decisiones reducen la inanición?

> [!answer]- Respuesta
> Semáforos, locks y colas se construyen con equidad; las secciones críticas son breves; se usa `notifyAll/signalAll`; y la mayoría de las esperas son bloqueantes. La equidad reduce el riesgo pero no garantiza por sí sola que el planificador ejecute siempre a cada hilo.

### 74. ¿Hay riesgo de livelock en los reintentos?

> [!answer]- Respuesta
> Los bucles de reintento podrían competir repetidamente bajo mucha carga, pero incluyen una pausa de 20 ms, lo que reduce la actividad continua. No es una garantía formal. Una cola de espera o una condición señalizada eliminaría mejor ese patrón de reintentos.

### 75. ¿Por qué los recursos se liberan en `finally`?

> [!answer]- Respuesta
> Porque `finally` se ejecuta tanto en el camino normal como ante excepciones. Sin él, una interrupción podría perder un permiso o dejar un lock tomado, degradando capacidad o provocando bloqueo permanente.

### 76. ¿Cuál es el protocolo correcto ante `InterruptedException`?

> [!answer]- Respuesta
> Si el método no puede propagarla porque implementa `Runnable.run()`, debe restaurar el indicador con `Thread.currentThread().interrupt()`, limpiar recursos y finalizar o tomar una decisión explícita. El proyecto sigue ese patrón en los objetos activos.

### 77. ¿Se puede afirmar que nunca hay deadlock para cualquier entrada?

> [!answer]- Respuesta
> No. Sería una afirmación demasiado fuerte. La configuración de 40 visitantes completa las barreras, pero autitos, comedor y teatro pueden quedar con un grupo incompleto para otras cantidades o interrupciones. La defensa correcta es explicar qué se garantiza en el escenario configurado y qué política de cancelación falta para generalizarlo.

### 78. ¿Los campos deberían ser `final`?

> [!answer]- Respuesta
> Muchos campos de dependencias y mecanismos se asignan solo en el constructor y podrían declararse `final`. Eso documentaría inmutabilidad de la referencia y ayudaría a la publicación segura. El código funciona porque los objetos se construyen antes de `Thread.start()`, que publica sus escrituras a los nuevos hilos, pero `final` mejoraría claridad y robustez.

### 79. ¿Qué problema tienen los tipos sin genéricos?

> [!answer]- Respuesta
> `BlockingQueue` y `Exchanger` se usan como tipos crudos, por lo que se requieren conversiones y el compilador no puede impedir insertar un objeto incorrecto. Lo ideal sería `BlockingQueue<SolicitudViaje>` y `Exchanger<Object>` o, mejor aún, un protocolo tipado. La decisión se tomó para ajustarse al nivel de colecciones visto, pero sacrifica seguridad de tipos.

---

## 14. Preguntas sobre el enunciado y cobertura

### 80. ¿Dónde se usa cada mecanismo obligatorio?

> [!answer]- Respuesta
> `Semaphore`: molinetes, capacidades de atracciones y shopping. Monitor: estados y contador de `Parque`, además del registro. `ReentrantLock` + `Condition`: VR. `BlockingQueue`: transportes y premios. `Exchanger`: ficha/premio. `CyclicBarrier`: montaña, autitos, comedor y teatro. También se agregan `CountDownLatch`, `volatile` y `join`.

### 81. ¿Cómo se cubre “entrada y salida de visitantes”?

> [!answer]- Respuesta
> `Parque.ingresar` espera la apertura, rechaza después del cierre, cruza un molinete e incrementa el contador bajo el monitor. `Visitante` llama a `salir` en `finally`, que decrementa y notifica. El reloj espera ese contador en cero antes del cierre final.

### 82. ¿Cómo se escalan los horarios?

> [!answer]- Respuesta
> El reloj duerme intervalos de milisegundos y luego informa 09:00, 18:00, 19:00 y 23:00. Es una escala lógica para terminar la demostración en segundos. Los cinco minutos del tren y demás duraciones también se representan con esperas mucho menores.

### 83. ¿Qué entregables exige la consigna y dónde están?

> [!answer]- Respuesta
> Código fuente Java en `src/`; instrucciones de compilación y ejecución en [[README]]; explicación de mecanismos en [[REPORTE]] y un análisis ampliado en [[DECISIONES_DE_SINCRONIZACION]]. Esta guía complementa la preparación oral.

### 84. ¿Se usaron bibliotecas externas?

> [!answer]- Respuesta
> No. Se usa el JDK y especialmente `java.util.concurrent`: `Semaphore`, `CyclicBarrier`, `BlockingQueue`, `ArrayBlockingQueue`, `Exchanger`, `CountDownLatch`, `ReentrantLock`, `Condition` y `TimeUnit`.

### 85. ¿Cómo compilarías y ejecutarías el proyecto?

> [!answer]- Respuesta
> Desde la raíz: `mkdir -p bin`, luego `javac -d bin src/*.java` y finalmente `java -cp bin SimulacionParque`. Se requiere JDK 8 o posterior.

---

## 15. Preguntas críticas y mejoras

### 86. ¿Cuál es la principal fortaleza técnica?

> [!answer]- Respuesta
> Cada problema se modela con una abstracción adecuada y la sincronización queda encapsulada: permisos para capacidad, barreras para grupos, colas para productor–consumidor, latches para finalización individual, exchanger para canje y condición para recursos compuestos. No se fuerza un único mecanismo para todo.

### 87. ¿Cuál es la principal limitación funcional?

> [!answer]- Respuesta
> El ciclo horario no gobierna realmente el comienzo de cada actividad: el cierre de las 19:00 se registra pero no se consulta durante el recorrido. Además, algunas barreras dependen de que la cantidad de visitantes complete exactamente los grupos.

### 88. ¿Cómo harías la simulación válida para cualquier cantidad de visitantes?

> [!answer]- Respuesta
> Separaría cada turno o función como una generación explícita con estado protegido; agregaría cierres y esperas temporizadas; al cerrar, aceptaría un grupo parcial donde la consigna lo permita o cancelaría y liberaría de forma coordinada donde exija grupo completo. También evitaría que nuevos visitantes entren a una generación que está cerrándose.

### 89. ¿Cómo mejorarías el manejo de horarios?

> [!answer]- Respuesta
> Pasaría un estado de ciclo de vida a cada atracción o les enviaría una operación `cerrar()`. Antes de encolar o adquirir capacidad comprobarían el estado bajo la misma sincronización que la admisión. Los usuarios ya admitidos terminarían; los nuevos serían rechazados. A las 23:00 se podría activar una evacuación explícita si esa fuera la política requerida.

### 90. ¿Cómo probarías invariantes concurrentes además de mirar prints?

> [!answer]- Respuesta
> Agregaría contadores internos y aserciones: ocupación nunca negativa ni superior a capacidad, equipos de VR conservados, grupos con tamaño correcto, ninguna solicitud completada dos veces y `personasDentro == 0` al final. Ejecutaría muchas iteraciones, con distintas cantidades, tiempos aleatorios e interrupciones, usando timeouts para detectar bloqueos.

### 91. ¿Los prints prueban que no existen carreras?

> [!answer]- Respuesta
> No. Sirven para observar comportamiento y depurar, pero cambian el timing y no exploran todos los intercalados. La corrección se argumenta mediante invariantes, regiones protegidas y contratos de los mecanismos; luego se complementa con pruebas repetidas y herramientas de análisis.

### 92. ¿Qué cambiarías para mejorar mantenibilidad?

> [!answer]- Respuesta
> Usaría genéricos, campos `final`, constantes o configuración para los tiempos, una interfaz común de cierre para servicios, estados horarios explícitos y pruebas automatizadas. También separaría el itinerario del visitante mediante una estrategia para poder probar recorridos diversos.

### 93. ¿Qué pasaría si hubiera varios conductores para el mismo transporte?

> [!answer]- Respuesta
> La cola repartiría solicitudes de forma segura, pero podrían coexistir viajes y se perdería la idea de un único vehículo, porque no hay un semáforo que limite la cantidad de viajes activos. El diseño presupone exactamente un hilo encargado por instancia de transporte.

### 94. ¿Qué pasaría si se ejecutara `cerrar()` mientras llegan nuevas solicitudes?

> [!answer]- Respuesta
> La bandera detiene al consumidor cuando está en `false` y la cola se ve vacía, pero `viajar()` no comprueba el cierre. Existe una carrera donde una solicitud podría encolarse después de que el encargado decidió terminar y quedar esperando su latch. En el flujo actual el principal cierra solo después del `join` de visitantes, así esa carrera no ocurre. Para una API general, la admisión y el cierre deberían coordinarse atómicamente.

### 95. ¿Por qué conviene reconocer límites en vez de negarlos?

> [!answer]- Respuesta
> Porque demuestra comprensión de las garantías reales. Una defensa sólida distingue escenario de prueba, invariante garantizada y supuesto. Después propone una mejora coherente. Afirmar ausencia absoluta de bloqueos contradice las barreras sin cierre y es más fácil de refutar.

---

## 16. Recorrido rápido para una exposición oral

### 96. ¿Cómo presentarías el trabajo en dos minutos?

> [!answer]- Respuesta sugerida
> “Modelamos cada visitante como un hilo y dejamos que cada atracción encapsule su sincronización. El parque es un monitor para apertura, cierre y cantidad de personas, mientras un semáforo representa los molinetes. Para capacidades usamos semáforos; para grupos exactos, barreras cíclicas; para filas de barco, tren y premios, colas bloqueantes. Cada pasajero espera el fin de su viaje con un latch, y premios usa un exchanger privado para canjear ficha y premio. En realidad virtual usamos un lock con condición para reservar atómicamente un visor, dos manoplas y una base, evitando retención parcial. El principal inicia todos los hilos, espera con join y los cierra mediante banderas o centinelas. Las trazas identifican tiempo e hilo. La configuración de 40 visitantes completa los grupos; como mejora, generalizaríamos el cierre de barreras y haríamos efectivo el cierre de actividades a las 19:00.”

### 97. ¿Qué frase resume el criterio para elegir mecanismos?

> [!answer]- Respuesta
> “No elegimos mecanismos solo para cumplir una lista: cada uno representa una condición distinta. El semáforo cuenta capacidad, la barrera reúne un grupo, la cola ordena trabajo, el latch señala una finalización, el exchanger hace un canje y la condición espera una guarda sobre estado compuesto.”

### 98. ¿Qué archivos conviene tener abiertos durante la defensa?

> [!answer]- Respuesta
> [[src/SimulacionParque.java]], [[src/Visitante.java]], [[src/Parque.java]], [[src/TransporteConCola.java]], [[src/JuegosDePremios.java]], [[src/RealidadVirtual.java]] y [[src/Registro.java]]. Con ellos se muestran arquitectura, monitor, cola/latch, exchanger, lock/condition y trazas; las atracciones con barrera pueden abrirse cuando pregunten específicamente.

---

## Lista de comprobación antes de defender

- [ ] Puedo explicar la diferencia entre capacidad y formación de grupo.
- [ ] Sé por qué `while` rodea a `wait/await`.
- [ ] Sé explicar `finally`, interrupciones y `join`.
- [ ] Puedo ubicar cada mecanismo obligatorio en una clase.
- [ ] Puedo describir un invariante por atracción.
- [ ] Reconozco el cierre de actividades no aplicado y los grupos incompletos.
- [ ] Sé proponer una mejora sin afirmar que ya está implementada.
- [ ] Puedo explicar por qué el orden de los prints cambia.
- [ ] Probé compilar y ejecutar desde una terminal con un JDK.

> [!tip] Regla útil para responder
> Primero nombrá la condición del problema, después el mecanismo, luego la garantía y al final el límite. Ejemplo: “La montaña necesita cinco personas exactas; uso una barrera de cinco; nadie continúa antes del quinto; para cantidades arbitrarias necesitaría una política de cancelación”.
