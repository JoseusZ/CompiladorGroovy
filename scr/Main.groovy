/**
 * script principal que ejecuta las fases del compilador analisis lexico, sintactico y ejecucion
 */

class Main {

    static void main(String[] args) {
        println "=================================================="
        println "  Compilador Groovy (Lexer + Parser + Interprete)  "
        println "=================================================="

        String modo = (args.length > 0 ? args[0] : "--stdin").toLowerCase()
        String argumento = (args.length > 1 ? args[1] : null)

        String fuente = null

        try {
            switch (modo) {
                case "--stdin":
                case "-i":
                    println "\n[main] MODO CONSOLA: escribe o pega tu codigo. Termina con una linea que contenga solo 'EOF'."
                    println "--------------------------------------------------------"
                    fuente = leerDesdeStdin()
                    break
                case "--file":
                case "-f":
                    if (!argumento) {
                        println "[main] uso: --file <ruta/al/archivo.groovy>"
                        return
                    }
                    File f = new File(argumento)
                    if (!f.exists()) {
                        println "[main] no existe el archivo: " + argumento
                        return
                    }
                    fuente = f.text
                    println "\n[main] MODO ARCHIVO: " + argumento
                    break
                case "--demo":
                case "-d":
                    fuente = obtenerDemo()
                    println "\n[main] MODO DEMO: programa de ejemplo embebido"
                    break
                default:
                    println "[main] modo desconocido: " + modo
                    println "        opciones validas: --stdin | --file <ruta> | --demo"
                    return
            }

            if (fuente == null) {
                println "\n[main] no se recibio codigo fuente. Abortando."
                return
            }

            // Eco del input en la propia consola para que veas lo que llega
            println "\n=================== TU CODIGO ==================="
            println fuente
            println "================================================="

            // Fase 1: Lexer puro en memoria (sin tocar disco para los tokens)
            println "\n[Fase 1] Analisis lexico en memoria..."
            AnalyzerLex lexer = new AnalyzerLex()
            List<String> tokens = lexer.analizar(fuente)

            if (tokens == null || tokens.isEmpty()) {
                println "[main] no se generaron tokens (¿el codigo esta vacio o solo tiene comentarios?)"
                return
            }
            println "[Fase 1] tokens generados: " + tokens.size()

            // Fase 2: Parser recibe tokens en memoria
            println "\n[Fase 2] Analisis sintactico + construccion del AST..."
            AnalyzerParser parser = new AnalyzerParser()
            def raizAST = parser.analizar(tokens)

            println "\n--------------------------------------------------"
            if (raizAST != null && !parser.hayErrores) {
                println "[Fase 2] OK: el codigo es sintacticamente correcto"
                println "[Fase 3] Ejecutando el AST..."

                println "\n=========== SALIDA DE TU PROGRAMA ==========="
                try {
                    Environment env = new Environment()
                    raizAST.ejecutar(env)
                    println "\n=============================================="
                    println "[main] ejecucion finalizada"
                } catch (ExcepcionReturn ret) {
                    println "\n=============================================="
                    println "[main] ejecucion finalizada (return = " + ret.valor + ")"
                } catch (Exception runtimeError) {
                    println "\n=============================================="
                    println "[runtime error] " + runtimeError.message
                }
            } else {
                println "[main] se encontraron errores de sintaxis. Compilacion abortada."
            }
            println "--------------------------------------------------"

        } catch (Exception e) {
            println "\n[main] error critico: " + e.message
        }
    }

    /**
     * Lee el codigo fuente desde stdin.
     *
     * Estrategia:
     *  - Si NO hay consola interactiva (pipe, redireccion o modo "trabajo por
     *    lotes" de cmd.exe en Windows), se lee todo System.in de una sola
     *    vez. Esto evita el molesto prompt
     *        "Terminar el programa por lotes (S/N)?"
     *    que cmd lanza cuando un proceso lee stdin sin TTY real.
     *  - Si SI hay consola interactiva, se mantiene el bucle por lineas
     *    terminado con una linea que contenga solo 'EOF'.
     *
     * En ambos casos, si la entrada trae una linea 'EOF' al final, se
     * descarta para que no aparezca en el codigo fuente.
     */
    private static String leerDesdeStdin() {
        String textoCrudo
        try {
            if (System.console() == null) {
                // Sin TTY: leer todo de golpe. Sin BufferedReader.readLine()
                // que es lo que dispara el prompt de cmd.
                textoCrudo = System.in.text
            } else {
                // Con TTY: lectura interactiva linea a linea.
                StringBuilder sb = new StringBuilder()
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
                String linea
                while ((linea = reader.readLine()) != null) {
                    if (linea.trim() == "EOF") break
                    sb.append(linea).append('\n')
                }
                textoCrudo = sb.toString()
            }
        } catch (Exception ioe) {
            println "[main] no se pudo leer stdin: " + ioe.message
            return ""
        }

        return limpiarEofFinal(textoCrudo)
    }

    /**
     * Quita una linea final 'EOF' (con o sin espacios) y normaliza saltos.
     */
    private static String limpiarEofFinal(String texto) {
        if (texto == null) return ""
        String normalizado = texto.replaceAll('\r\n', '\n').replaceAll('\r', '\n')
        // Quitar la linea EOF final si existe
        normalizado = normalizado.replaceAll(/(?m)\s*\bEOF\b\s*$/, '')
        return normalizado.trim()
    }

    /**
     * Programa de demostracion embebido, util para 'probar sin teclear nada'.
     */
    private static String obtenerDemo() {
        return "int a = 1\n" +
                "int b = 2\n" +
                "int suma = a + b\n" +
                "print(suma)\n"
    }
}