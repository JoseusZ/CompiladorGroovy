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
     * Lee lineas desde stdin hasta que el usuario escriba 'EOF' en una linea sola.
     * Asi funciona tanto en consola interactiva como cuando se redirige con pipes.
     */
    private static String leerDesdeStdin() {
        StringBuilder sb = new StringBuilder()
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))
        String linea
        while ((linea = reader.readLine()) != null) {
            if (linea.trim() == "EOF") break
            sb.append(linea).append('\n')
        }
        return sb.toString().trim()
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