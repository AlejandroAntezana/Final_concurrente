# Parque de Diversiones - Programacion Concurrente

Simulacion del trabajo practico obligatorio realizada en Java, sin bibliotecas externas.

## Requisitos

- JDK 8 o posterior.
- Una terminal ubicada en la carpeta raiz del proyecto.

## Compilar y ejecutar

En Linux/macOS:

```bash
mkdir -p bin
javac -d bin src/*.java
java -cp bin SimulacionParque
```

En Windows, reemplace `:` por `;` solamente si agrega mas elementos al classpath. El comando
mostrado posee un unico elemento y funciona como `java -cp bin SimulacionParque`.

La simulacion crea 40 visitantes, tres molinetes y los empleados necesarios. Los tiempos reales
estan reducidos: el reloj imprime las horas importantes del dia sin obligar a esperar catorce
horas. Debido a la planificacion concurrente de los hilos, el orden de los mensajes cambia entre
ejecuciones.

## Estructura

- `SimulacionParque`: crea y pone en marcha todos los hilos y recursos compartidos.
- `Visitante`, `RelojParque` y `EncargadoPremios`: objetos activos (`Runnable`).
- Las restantes clases representan el parque y sus areas, y encapsulan la sincronizacion.

El detalle de las decisiones se encuentra en `REPORTE.md`.
