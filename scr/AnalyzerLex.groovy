import java.util.regex.Matcher
import java.io.File

/**
 * analizador lexico para tokenizacion de codigo
 */
class AnalyzerLex {

    /**
     * Devuelve el último bloque de tokens generado, en formato "Linea N | <TIPO, valor>".
     * Permite a Main.groovy alimentar al parser sin pasar por un archivo.
     */
    List<String> ultimosTokens = []

    /**
     * Modo archivo (legacy): mantiene compatibilidad con quien pase una ruta.
     * Delega en el método puro analizar(String fuente).
     */
    void analizarTokens(String nombreArchivo) {
        File archivo = new File(nombreArchivo).absoluteFile

        if (!archivo.exists()) {
            println 'Error: El archivo no existe en la ruta ' + archivo.absolutePath
            return
        }

        String fuente = archivo.text
        List<String> tokens = analizar(fuente)

        if (tokens == null) return

        // Persistencia opcional: deja un rastro en disco para depuración
        try {
            String rutaBase = System.getProperty("user.dir")
            File archivoSalida = new File(rutaBase + "/output/tokens_verticales.txt")
            archivoSalida.parentFile.mkdirs()
            archivoSalida.text = tokens.join('\n')
        } catch (Exception ignored) {
            // Si no se puede escribir el archivo de tokens, el parser aun asi los recibira en memoria
        }
    }

    /**
     * Nucleo del lexer: recibe el codigo fuente en memoria y devuelve la lista de tokens.
     * Esta es la API preferida para uso en consola.
     */
    List<String> analizar(String fuente) {
        List<String> listaTokens = []
        boolean enComentarioBloque = false

        // lista de palabras reservadas
        List<String> palabrasReservadas = [
                'as', 'assert', 'break', 'case', 'catch', 'class', 'const', 'continue',
                'def', 'default', 'do', 'else', 'enum', 'extends', 'false', 'finally',
                'for', 'goto', 'if', 'implements', 'import', 'in', 'instanceof', 'interface',
                'new', 'null', 'package', 'return', 'super', 'switch', 'this', 'throw',
                'throws', 'trait', 'true', 'try', 'var', 'void', 'volatile', 'while',
                'int', 'double', 'float', 'boolean', 'char', 'byte', 'short', 'long',
                'synchronized', 'transient', 'native', 'strictfp', 'yield', 'record', 'sealed', 'permits',
                'print', 'println'
        ]

        try {
            List<String> lineas = fuente.split('\n') as List<String>

            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas[i]
                int numLinea = i + 1
                String lineaLimpia = linea.trim()

                if (!lineaLimpia) {
                    continue
                }

                if (numLinea == 1 && lineaLimpia.startsWith('#!')) {
                    continue
                }

                if (enComentarioBloque) {
                    if (lineaLimpia.contains('*/')) {
                        enComentarioBloque = false
                        lineaLimpia = lineaLimpia.substring(lineaLimpia.indexOf('*/') + 2)
                    } else {
                        continue
                    }
                }

                if (lineaLimpia.contains('/*')) {
                    if (lineaLimpia.contains('*/')) {
                        lineaLimpia = lineaLimpia.replaceAll(/\/\*.*?\*\//, '')
                    } else {
                        enComentarioBloque = true
                        lineaLimpia = lineaLimpia.substring(0, lineaLimpia.indexOf('/*'))
                    }
                }

                if (lineaLimpia.contains('//')) {
                    lineaLimpia = lineaLimpia.substring(0, lineaLimpia.indexOf('//'))
                }

                if (!lineaLimpia.trim()) {
                    continue
                }

                String stringsMultilinea = '\"\"\"(?:[^\"\\\\]|\\\\.)*\"\"\"|\'\'\'(?:[^\'\\\\]|\\\\.)*\'\'\''
                String stringsSimples    = '\"(?:[^\"\\\\]|\\\\.)*\"|\'(?:[^\'\\\\]|\\\\.)*\''
                String idInvalidos       = '\\d+[a-zA-Z_$]\\w*'
                String numeros           = '0[xX][0-9a-fA-F]+|\\d+\\.\\d+(?:[eE][+-]?\\d+)?|\\d+'
                String identificadores   = '[a-zA-Z_$]\\w*'

                String opCompuestos1     = '<=>|==~|=~|\\.\\.|\\?\\.|[=!]=|<=|>=|&&|\\|\\||\\+\\+|--'
                String opCompuestos2     = '\\+=|-=|\\*=|\\/=|%=|\\?:|->|<<|>>|>>>'

                String opSimples         = '[-+*\\/%=<>!&|^~?]'
                String delimitadores     = '[().,;:\\[\\]{}]'

                String patronTokens = stringsMultilinea + '|' + stringsSimples + '|' +
                        idInvalidos + '|' + numeros + '|' + identificadores + '|' +
                        opCompuestos1 + '|' + opCompuestos2 + '|' +
                        opSimples + '|' + delimitadores + '|\\S'

                Matcher matcher = (lineaLimpia =~ patronTokens)

                while (matcher.find()) {
                    String token = matcher.group()
                    String tipoToken = 'DESCONOCIDO'

                    if (token ==~ /^\d+[a-zA-Z_$].*/) {
                        println "Error Léxico [Línea ${numLinea}]: Identificador inválido '${token}'"
                        listaTokens.add("Linea ${numLinea} | <ERROR, ${token}>")
                        continue
                    }

                    if (token.startsWith('"') || token.startsWith("'")) {
                        tipoToken = 'CADENA'
                    } else if (palabrasReservadas.contains(token)) {
                        tipoToken = 'PR_' + token.toUpperCase()
                    } else if (token ==~ /^(0[xX][0-9a-fA-F]+|\d+\.\d+(?:[eE][+-]?\d+)?|\d+)$/) {
                        if (token.contains('.')) {
                            tipoToken = 'NUM_DECIMAL'
                        } else {
                            tipoToken = 'NUM_ENTERO'
                        }
                    } else if (token ==~ /^[a-zA-Z_$][a-zA-Z0-9_$]*$/) {
                        tipoToken = 'IDENTIFICADOR'
                    } else if (token == '[') {
                        tipoToken = 'CORCHETE_APERTURA'
                    } else if (token == ']') {
                        tipoToken = 'CORCHETE_CIERRE'
                    } else if (token == '{') {
                        tipoToken = 'LLAVE_APERTURA'
                    } else if (token == '}') {
                        tipoToken = 'LLAVE_CIERRE'
                    } else if (token == '(') {
                        tipoToken = 'PARENTESIS_APERTURA'
                    } else if (token == ')') {
                        tipoToken = 'PARENTESIS_CIERRE'
                    } else if (token ==~ /^[.,;:]$/) {
                        tipoToken = 'DELIMITADOR'
                    } else if (esOperador(token)) {
                        tipoToken = 'OPERADOR'
                    } else {
                        println "Advertencia Léxica [Línea ${numLinea}]: Símbolo no reconocido '${token}'"
                    }

                    listaTokens.add("Linea ${numLinea} | <${tipoToken}, ${token}>")
                }
            }

            this.ultimosTokens = listaTokens
            return listaTokens

        } catch (Exception e) {
            println 'Ocurrio un error inesperado en el Lexer: ' + e.message
            return null
        }
    }

    /**
     * Variante historica mantenida por compatibilidad si alguien la usa directamente.
     * Internamente delega en el método puro.
     */
    @SuppressWarnings(['JavaIoPackageAccess', 'CatchException', 'PrintStackTrace', 'MethodSize'])
    void analizarTokensLegacy(String fuenteArchivo) {
        File archivo = new File(fuenteArchivo).absoluteFile
        if (!archivo.exists()) {
            println 'Error: El archivo no existe en la ruta ' + archivo.absolutePath
            return
        }
        analizar(archivo.text)
    }

    private boolean esOperador(String token) {
        def operadores = [
                '<=>', '==~', '=~', '..', '?.', '==', '!=', '<=', '>=',
                '&&', '||', '++', '--', '+=', '-=', '*=', '/=', '%=',
                '?:', '->', '<<', '>>', '>>>', '=', '+', '-', '*', '/',
                '%', '<', '>', '!', '&', '|', '^', '~', '?'
        ]
        return operadores.contains(token)
    }
}