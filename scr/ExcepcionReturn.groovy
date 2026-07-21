/**
 * ExcepcionReturn.groovy
 *
 * Signal usado por NodoReturn para abortar el cuerpo de la funcion en ejecucion
 * y propagar el valor de retorno al NodoLlamadaFuncion que lo solicito.
 * Extiende RuntimeException para no requerir declaracion en cada 'throws'.
 *
 * Antes vivia dentro de Environment.groovy, pero al separar archivos por
 * responsabilidad mejora el orden de carga cuando groovy resuelve clases
 * a partir de classpath. Asi cualquier archivo puede capturarlo sin
 * depender transitivamente de Environment.
 */
class ExcepcionReturn extends RuntimeException {
    Object valor

    ExcepcionReturn(Object valor) {
        // (message, cause, enableSuppression, writableStackTrace) -> todos vacios
        // porque esta excepcion NO es un error real, solo un canal de control.
        super(null, null, false, false)
        this.valor = valor
    }
}
