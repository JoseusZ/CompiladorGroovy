// =========================================================
// CLASES AUXILIARES Y NODOS AST (EL "CEREBRO" DE LA MEMORIA)
// =========================================================

class Token {
    String tipo
    String valor
    int linea
}

abstract class NodoAST {
    abstract Object ejecutar(Environment env)
}

class NodoVacio extends NodoAST {
    @Override
    Object ejecutar(Environment env) { return null }
}

class NodoBloque extends NodoAST {
    List<NodoAST> instrucciones = []
    @Override
    Object ejecutar(Environment env) {
        Object ultimoValor = null
        for (NodoAST inst : instrucciones) {
            if (inst != null) ultimoValor = inst.ejecutar(env)
        }
        return ultimoValor
    }
}

class NodoNumero extends NodoAST {
    Double valor
    NodoNumero(Double valor) { this.valor = valor }
    @Override
    Object ejecutar(Environment env) { return valor }
}

class NodoBooleano extends NodoAST {
    Boolean valor
    NodoBooleano(Boolean valor) { this.valor = valor }
    @Override
    Object ejecutar(Environment env) { return valor }
}

class NodoCadena extends NodoAST {
    String valor
    NodoCadena(String valor) { this.valor = valor.replace("\"", "") }
    @Override
    Object ejecutar(Environment env) { return valor }
}

class NodoVariable extends NodoAST {
    String nombre
    NodoVariable(String nombre) { this.nombre = nombre }
    @Override
    Object ejecutar(Environment env) { return env.obtener(nombre) }
}

class NodoAsignacion extends NodoAST {
    String nombreVariable
    NodoAST valorExpresion
    NodoAsignacion(String nombreVariable, NodoAST valorExpresion) {
        this.nombreVariable = nombreVariable
        this.valorExpresion = valorExpresion
    }
    @Override
    Object ejecutar(Environment env) {
        Object valorCalculado = valorExpresion.ejecutar(env)
        env.asignar(nombreVariable, valorCalculado)
        return valorCalculado
    }
}

class NodoPrint extends NodoAST {
    NodoAST expresion
    boolean saltoLinea
    NodoPrint(NodoAST expresion, boolean saltoLinea) {
        this.expresion = expresion
        this.saltoLinea = saltoLinea
    }
    @Override
    Object ejecutar(Environment env) {
        Object resultado = expresion.ejecutar(env)
        if (saltoLinea) println(resultado)
        else print(resultado)
        return null
    }
}

class NodoOperacionBinaria extends NodoAST {
    NodoAST izq
    String operador
    NodoAST der
    NodoOperacionBinaria(NodoAST izq, String operador, NodoAST der) {
        this.izq = izq
        this.operador = operador
        this.der = der
    }
    @Override
    Object ejecutar(Environment env) {
        def valIzq = izq.ejecutar(env)
        def valDer = der.ejecutar(env)

        if (operador == "+") return valIzq + valDer
        if (operador == "-") return valIzq - valDer
        if (operador == "*") return valIzq * valDer
        if (operador == "/") return valIzq / valDer

        if (operador == "<") return (valIzq as Double) < (valDer as Double)
        if (operador == ">") return (valIzq as Double) > (valDer as Double)
        if (operador == "<=") return (valIzq as Double) <= (valDer as Double)
        if (operador == ">=") return (valIzq as Double) >= (valDer as Double)
        if (operador == "==") return valIzq == valDer
        if (operador == "!=") return valIzq != valDer

        if (operador == "&&") return (valIzq as Boolean) && (valDer as Boolean)
        if (operador == "||") return (valIzq as Boolean) || (valDer as Boolean)

        return null
    }
}

class NodoIf extends NodoAST {
    NodoAST condicion
    NodoAST bloqueVerdadero
    NodoAST bloqueFalso
    NodoIf(NodoAST condicion, NodoAST bloqueVerdadero, NodoAST bloqueFalso) {
        this.condicion = condicion
        this.bloqueVerdadero = bloqueVerdadero
        this.bloqueFalso = bloqueFalso
    }
    @Override
    Object ejecutar(Environment env) {
        def resCondicion = condicion.ejecutar(env)
        boolean esVerdadero = false
        if (resCondicion instanceof Boolean) esVerdadero = resCondicion
        else if (resCondicion instanceof Number) esVerdadero = resCondicion.doubleValue() != 0.0

        if (esVerdadero) return bloqueVerdadero.ejecutar(env)
        else if (bloqueFalso != null) return bloqueFalso.ejecutar(env)
        return null
    }
}

class NodoWhile extends NodoAST {
    NodoAST condicion
    NodoAST bloque
    NodoWhile(NodoAST condicion, NodoAST bloque) {
        this.condicion = condicion
        this.bloque = bloque
    }
    @Override
    Object ejecutar(Environment env) {
        Object ultimoValor = null
        while (true) {
            def resCondicion = condicion.ejecutar(env)
            boolean esVerdadero = false
            if (resCondicion instanceof Boolean) esVerdadero = resCondicion
            else if (resCondicion instanceof Number) esVerdadero = resCondicion.doubleValue() != 0.0

            if (!esVerdadero) break
            ultimoValor = bloque.ejecutar(env)
        }
        return ultimoValor
    }
}

class NodoArreglo extends NodoAST {
    List<NodoAST> elementos
    NodoArreglo(List<NodoAST> elementos) { this.elementos = elementos }
    @Override
    Object ejecutar(Environment env) {
        List<Object> listaEvaluada = new ArrayList<>()
        for (NodoAST nodo : elementos) {
            listaEvaluada.add(nodo.ejecutar(env))
        }
        return listaEvaluada
    }
}

class NodoAccesoArreglo extends NodoAST {
    String nombreVariable
    NodoAST indice
    NodoAccesoArreglo(String nombreVariable, NodoAST indice) {
        this.nombreVariable = nombreVariable
        this.indice = indice
    }
    @Override
    Object ejecutar(Environment env) {
        def lista = env.obtener(nombreVariable)
        def idxEvaluado = indice.ejecutar(env)
        if (lista instanceof List && idxEvaluado instanceof Number) {
            int i = idxEvaluado.intValue()
            if (i >= 0 && i < lista.size()) return lista.get(i)
            else throw new Exception("Error: Indice " + i + " fuera de limites para el arreglo '" + nombreVariable + "'")
        }
        throw new Exception("Error: La variable '" + nombreVariable + "' no es un arreglo o el indice es invalido")
    }
}

class NodoReturn extends NodoAST {
    NodoAST valorExpresion
    NodoReturn(NodoAST valorExpresion) { this.valorExpresion = valorExpresion }
    @Override
    Object ejecutar(Environment env) {
        Object valor = null
        if (valorExpresion != null) valor = valorExpresion.ejecutar(env)
        throw new ExcepcionReturn(valor)
    }
}

class NodoFuncionDeclaracion extends NodoAST {
    String nombre
    List<String> parametros
    NodoAST bloqueCuerpo
    NodoFuncionDeclaracion(String nombre, List<String> parametros, NodoAST bloqueCuerpo) {
        this.nombre = nombre
        this.parametros = parametros
        this.bloqueCuerpo = bloqueCuerpo
    }
    @Override
    Object ejecutar(Environment env) {
        env.asignar(nombre, this)
        return null
    }
}

class NodoLlamadaFuncion extends NodoAST {
    String nombreFuncion
    List<NodoAST> argumentos
    NodoLlamadaFuncion(String nombreFuncion, List<NodoAST> argumentos) {
        this.nombreFuncion = nombreFuncion
        this.argumentos = argumentos
    }
    @Override
    Object ejecutar(Environment env) {
        Object objFuncion = env.obtener(nombreFuncion)
        if (!(objFuncion instanceof NodoFuncionDeclaracion)) {
            throw new Exception("Error: '" + nombreFuncion + "' no es una funcion invocable.")
        }
        NodoFuncionDeclaracion declaracion = (NodoFuncionDeclaracion) objFuncion
        if (argumentos.size() != declaracion.parametros.size()) {
            throw new Exception("Error: La funcion '" + nombreFuncion + "' esperaba " + declaracion.parametros.size() + " argumentos pero recibio " + argumentos.size())
        }

        List<Object> valoresArgumentos = []
        for (NodoAST arg : argumentos) {
            valoresArgumentos.add(arg.ejecutar(env))
        }

        Environment entornoLocal = new Environment(env)
        for (int i = 0; i < declaracion.parametros.size(); i++) {
            entornoLocal.asignar(declaracion.parametros.get(i), valoresArgumentos.get(i))
        }

        try {
            declaracion.bloqueCuerpo.ejecutar(entornoLocal)
        } catch (ExcepcionReturn retorno) {
            return retorno.valor
        }
        return null
    }
}

// =========================================================
// ANALIZADOR SINTACTICO (EL CONSTRUCTOR DEL ARBOL)
// =========================================================

@SuppressWarnings(['JavaIoPackageAccess', 'CatchException'])
class AnalyzerParser {
    List<Token> listaTokens = []
    int apuntador = 0
    boolean hayErrores = false

    /**
     * Punto de entrada principal: el caller (Main.groovy) entrega los tokens crudos
     * emitidos por el lexer. Asi no dependemos de un archivo en disco para nada.
     */
    NodoAST analizar(List<String> tokensCrudos) {
        this.listaTokens = []
        this.apuntador = 0
        this.hayErrores = false
        construirListaTokens(tokensCrudos)
        return ejecutarPipeline()
    }

    /**
     * Variante legacy que conserva el contrato basado en archivo de tokens.
     */
    NodoAST analizar(String rutaArchivoTokens) {
        File archivo = new File(rutaArchivoTokens)

        if (!archivo.exists()) {
            println "[parser] error no se encontro el archivo de tokens"
            return null
        }

        List<String> lineasList = archivo.readLines()
        println "[parser] archivo cargado preparando extraccion de tokens"

        this.listaTokens = []
        this.apuntador = 0
        this.hayErrores = false
        construirListaTokens(lineasList)

        return ejecutarPipeline()
    }

    private void construirListaTokens(List<String> crudos) {
        for (String lineaStr : crudos) {
            lineaStr = lineaStr.trim()
            if (!lineaStr) continue

            if (lineaStr.startsWith("-") || lineaStr.startsWith("inicio") ||
                    lineaStr.startsWith("leyendo") || lineaStr.startsWith("analisis") ||
                    lineaStr.startsWith("total") || lineaStr.startsWith("resultado")) {
                continue
            }

            int separadorLinea = lineaStr.indexOf('|')
            int inicio = lineaStr.indexOf('<')
            int fin = lineaStr.lastIndexOf('>')

            if (separadorLinea != -1 && inicio != -1 && fin != -1 && fin > inicio) {
                int lineaReal = 0
                try {
                    lineaReal = Integer.parseInt(lineaStr.substring(0, separadorLinea).trim().replace("Linea", "").trim())
                } catch (Exception ignored) {
                    lineaReal = 0
                }

                String contenido = lineaStr.substring(inicio + 1, fin)
                int separadorComa = contenido.indexOf(',')

                if (separadorComa != -1) {
                    Token t = new Token()
                    t.linea = lineaReal
                    t.tipo = contenido.substring(0, separadorComa).trim()
                    t.valor = contenido.substring(separadorComa + 1).trim()
                    this.listaTokens.add(t)
                }
            }
        }
    }

    private NodoAST ejecutarPipeline() {
        if (listaTokens.isEmpty()) {
            println "[parser] Analisis exitoso: El archivo esta vacio o solo contiene comentarios."
            return new NodoVacio()
        }

        println "[parser] se iniciara la construccion del arbol con " + listaTokens.size() + " tokens"

        try {
            NodoAST raiz = programa()

            if (apuntador < listaTokens.size() && !hayErrores) {
                Token sobrante = obtenerTokenActual()
                imprimirError("se encontro codigo fuera de lugar al final del archivo", sobrante)
                return null
            }
            return raiz
        } catch (Exception e) {
            return null
        }
    }

    boolean esMetodoDecl() {
        Token t0 = obtenerTokenActual()
        if (!esTipoDatoFuerte(t0)) return false
        Token t1 = verToken(1)

        if (t1?.tipo == "IDENTIFICADOR" && verToken(2)?.valor == "(") return true
        if (t1?.tipo == "CORCHETE_APERTURA" && verToken(2)?.tipo == "CORCHETE_CIERRE" &&
                verToken(3)?.tipo == "IDENTIFICADOR" && verToken(4)?.valor == "(") {
            return true
        }
        return false
    }

    NodoAST programa() {
        return listaDeclaraciones()
    }

    NodoAST listaDeclaraciones() {
        NodoBloque bloque = new NodoBloque()
        while (apuntador < listaTokens.size()) {
            NodoAST instruccion = declaracion()
            if (instruccion != null) {
                bloque.instrucciones.add(instruccion)
            }
        }
        return bloque
    }

    NodoAST declaracion() {
        Token actual = obtenerTokenActual()
        if (actual == null) return null

        if (actual.tipo == "PR_CLASS") {
            claseDecl()
            return new NodoVacio()
        } else if (esMetodoDecl()) {
            return metodoDecl() // ACTUALIZADO
        } else {
            return sentencia()
        }
    }

    void claseDecl() {
        coincidir("PR_CLASS", "class")
        coincidir("IDENTIFICADOR", null)
        coincidir("LLAVE_APERTURA", "{")
        listaMiembros()
        coincidir("LLAVE_CIERRE", "}")
    }

    void listaMiembros() {
        while (apuntador < listaTokens.size()) {
            Token actual = obtenerTokenActual()
            if (actual.valor == "}") break
            miembro()
        }
    }

    void miembro() {
        if (esMetodoDecl()) metodoDecl()
        else variableDecl()
    }

    NodoAST metodoDecl() {
        avanzar() // consume def o tipo
        Token actual = obtenerTokenActual()
        if (actual != null && actual.tipo == "CORCHETE_APERTURA") {
            avanzar()
            coincidir("CORCHETE_CIERRE", "]")
        }
        String nombre = obtenerTokenActual().valor
        coincidir("IDENTIFICADOR", null)
        coincidir("PARENTESIS_APERTURA", "(")
        List<String> parametros = listaParametrosOpt()
        coincidir("PARENTESIS_CIERRE", ")")
        NodoAST cuerpo = bloque()

        return new NodoFuncionDeclaracion(nombre, parametros, cuerpo)
    }

    List<String> listaParametrosOpt() {
        Token actual = obtenerTokenActual()
        if (actual != null && actual.valor != ")") return listaParametros()
        return []
    }

    List<String> listaParametros() {
        List<String> params = [parametro()]
        while (obtenerTokenActual()?.valor == ",") {
            avanzar()
            params.add(parametro())
        }
        return params
    }

    String parametro() {
        Token actual = obtenerTokenActual()
        if (esTipoDatoFuerte(actual) || (verToken(1)?.tipo == "IDENTIFICADOR")) {
            avanzar()
            Token sig = obtenerTokenActual()
            if (sig != null && sig.tipo == "CORCHETE_APERTURA") {
                avanzar()
                coincidir("CORCHETE_CIERRE", "]")
            }
        }
        String nombreParam = obtenerTokenActual().valor
        coincidir("IDENTIFICADOR", null)
        return nombreParam
    }

    NodoAST listaSentencias() {
        NodoBloque bloque = new NodoBloque()
        while (apuntador < listaTokens.size()) {
            Token actual = obtenerTokenActual()
            if (actual.valor == "}") break
            NodoAST stmt = sentencia()
            if (stmt != null) bloque.instrucciones.add(stmt)
        }
        return bloque
    }

    NodoAST bloque() {
        coincidir("LLAVE_APERTURA", "{")
        NodoAST blk = listaSentencias()
        coincidir("LLAVE_CIERRE", "}")
        return blk
    }

    NodoAST sentencia() {
        Token actual = obtenerTokenActual()
        if (actual == null) return null

        if (actual.tipo == "PR_IF") {
            return estructuraIf()
        } else if (actual.tipo == "PR_WHILE") {
            return estructuraWhile()
        } else if (actual.tipo == "PR_FOR") {
            estructuraFor()
            return new NodoVacio()
        } else if (actual.tipo == "PR_RETURN") {
            return estructuraReturn() // ACTUALIZADO
        } else if (actual.tipo == "LLAVE_APERTURA") {
            return bloque()
        } else if (actual.tipo == "PR_PRINT" || actual.tipo == "PR_PRINTLN") {
            return estructuraPrint()
        } else if (esTipoDatoFuerte(actual)) {
            return variableDecl()
        } else if (actual.tipo == "IDENTIFICADOR") {
            Token siguiente = verToken(1)
            if (siguiente?.tipo == "IDENTIFICADOR" ||
                    (siguiente?.tipo == "CORCHETE_APERTURA" && verToken(2)?.tipo == "CORCHETE_CIERRE" && verToken(3)?.tipo == "IDENTIFICADOR")) {
                return variableDecl()
            } else if (siguiente?.valor == "(") {
                return llamadaMetodoStmt() // ACTUALIZADO
            } else if (siguiente?.valor == "=" || siguiente?.valor == "+=" || siguiente?.valor == "-=" || siguiente?.valor == "[") {
                return asignacionStmt()
            } else {
                imprimirError("sentencia no reconocida empezando con identificador", actual)
            }
        } else {
            imprimirError("sentencia no reconocida", actual)
        }
        return null
    }

    NodoAST estructuraIf() {
        coincidir("PR_IF", "if")
        coincidir("PARENTESIS_APERTURA", "(")
        NodoAST cond = expresion()
        coincidir("PARENTESIS_CIERRE", ")")
        NodoAST blqVerdadero = bloque()

        NodoAST blqFalso = null
        Token actual = obtenerTokenActual()
        if (actual != null && actual.tipo == "PR_ELSE") {
            coincidir("PR_ELSE", "else")
            blqFalso = bloque()
        }
        return new NodoIf(cond, blqVerdadero, blqFalso)
    }

    NodoAST estructuraWhile() {
        coincidir("PR_WHILE", "while")
        coincidir("PARENTESIS_APERTURA", "(")
        NodoAST cond = expresion()
        coincidir("PARENTESIS_CIERRE", ")")
        NodoAST blq = bloque()

        return new NodoWhile(cond, blq)
    }

    void estructuraFor() {
        coincidir("PR_FOR", "for")
        coincidir("PARENTESIS_APERTURA", "(")
        variableDecl()
        expresion()
        coincidir("DELIMITADOR", ";")
        asignacion()
        coincidir("PARENTESIS_CIERRE", ")")
        bloque()
    }

    NodoAST variableDecl() {
        avanzar()
        Token actual = obtenerTokenActual()
        if (actual != null && actual.tipo == "CORCHETE_APERTURA") {
            avanzar()
            coincidir("CORCHETE_CIERRE", "]")
        }

        String nombreVariable = obtenerTokenActual().valor
        coincidir("IDENTIFICADOR", null)

        NodoAST valorInicial = null
        actual = obtenerTokenActual()
        if (actual != null && actual.valor == "=") {
            avanzar()
            valorInicial = expresion()
        }
        opcionalPuntoYComa()
        return new NodoAsignacion(nombreVariable, valorInicial ?: new NodoNumero(0.0))
    }

    NodoAST asignacionStmt() {
        NodoAST asig = asignacion()
        opcionalPuntoYComa()
        return asig
    }

    NodoAST asignacion() {
        String nombreVariable = obtenerTokenActual().valor
        coincidir("IDENTIFICADOR", null)

        Token actual = obtenerTokenActual()
        if (actual != null && actual.tipo == "CORCHETE_APERTURA") {
            avanzar()
            expresion()
            coincidir("CORCHETE_CIERRE", "]")
        }

        Token operador = obtenerTokenActual()
        if (operador != null && (operador.valor == "=" || operador.valor == "+=" || operador.valor == "-=")) {
            avanzar()
            NodoAST valorExpr = expresion()
            return new NodoAsignacion(nombreVariable, valorExpr)
        } else {
            imprimirError("se esperaba un operador de asignacion", operador)
        }
        return null
    }

    NodoAST estructuraReturn() {
        coincidir("PR_RETURN", "return")
        NodoAST exp = null
        Token actual = obtenerTokenActual()
        if (actual != null && actual.valor != ";" && actual.valor != "}") {
            exp = expresion()
        }
        opcionalPuntoYComa()
        return new NodoReturn(exp)
    }

    NodoAST estructuraPrint() {
        Token t = obtenerTokenActual()
        boolean salto = (t.tipo == "PR_PRINTLN")
        avanzar()
        coincidir("PARENTESIS_APERTURA", "(")
        NodoAST exp = expresion()
        coincidir("PARENTESIS_CIERRE", ")")
        opcionalPuntoYComa()
        return new NodoPrint(exp, salto)
    }

    NodoAST llamadaMetodoStmt() {
        String nombre = obtenerTokenActual().valor
        coincidir("IDENTIFICADOR", null)
        coincidir("PARENTESIS_APERTURA", "(")
        List<NodoAST> args = listaArgumentosOpt()
        coincidir("PARENTESIS_CIERRE", ")")
        opcionalPuntoYComa()
        return new NodoLlamadaFuncion(nombre, args)
    }

    List<NodoAST> listaArgumentosOpt() {
        List<NodoAST> args = []
        Token actual = obtenerTokenActual()
        if (actual != null && actual.valor != ")" && actual.valor != "]") {
            args.add(expresion())
            while (obtenerTokenActual()?.valor == ",") {
                avanzar()
                args.add(expresion())
            }
        }
        return args
    }

    NodoAST arregloLiteral() {
        coincidir("CORCHETE_APERTURA", "[")
        List<NodoAST> elementos = []
        Token actual = obtenerTokenActual()
        if (actual != null && actual.valor != "]") {
            elementos.add(expresion())
            while (obtenerTokenActual()?.valor == ",") {
                avanzar()
                elementos.add(expresion())
            }
        }
        coincidir("CORCHETE_CIERRE", "]")
        return new NodoArreglo(elementos)
    }

    NodoAST expresion() { return expLogicaOr() }

    NodoAST expLogicaOr() {
        NodoAST nodo = expLogicaAnd()
        while (obtenerTokenActual()?.valor == "||") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = expLogicaAnd()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST expLogicaAnd() {
        NodoAST nodo = expIgualdad()
        while (obtenerTokenActual()?.valor == "&&") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = expIgualdad()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST expIgualdad() {
        NodoAST nodo = expRelacional()
        while (obtenerTokenActual()?.valor == "==" || obtenerTokenActual()?.valor == "!=") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = expRelacional()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST expRelacional() {
        NodoAST nodo = expAritmetica()
        while (obtenerTokenActual()?.valor == "<" || obtenerTokenActual()?.valor == ">" ||
                obtenerTokenActual()?.valor == "<=" || obtenerTokenActual()?.valor == ">=") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = expAritmetica()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST expAritmetica() {
        NodoAST nodo = termino()
        while (obtenerTokenActual()?.valor == "+" || obtenerTokenActual()?.valor == "-") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = termino()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST termino() {
        NodoAST nodo = factor()
        while (obtenerTokenActual()?.valor == "*" || obtenerTokenActual()?.valor == "/" || obtenerTokenActual()?.valor == "%") {
            String op = obtenerTokenActual().valor
            avanzar()
            NodoAST der = factor()
            nodo = new NodoOperacionBinaria(nodo, op, der)
        }
        return nodo
    }

    NodoAST factor() {
        Token actual = obtenerTokenActual()
        if (actual == null) {
            imprimirError("se esperaba un valor pero se alcanzo el fin de archivo", null)
            return new NodoVacio()
        }

        if (actual.valor == "-") {
            avanzar()
            NodoAST nodoDerecho = factor()
            return new NodoOperacionBinaria(new NodoNumero(0.0), "-", nodoDerecho)
        } else if (actual.valor == "+") {
            avanzar()
            return factor()
        }

        if (actual.tipo == "PARENTESIS_APERTURA") {
            avanzar()
            NodoAST exp = expresion()
            coincidir("PARENTESIS_CIERRE", ")")
            return exp
        } else if (actual.tipo == "IDENTIFICADOR") {
            String nombreVar = actual.valor
            avanzar()
            Token siguiente = obtenerTokenActual()

            if (siguiente?.tipo == "PARENTESIS_APERTURA") {
                avanzar()
                List<NodoAST> args = listaArgumentosOpt()
                coincidir("PARENTESIS_CIERRE", ")")
                return new NodoLlamadaFuncion(nombreVar, args) // ACTUALIZADO
            } else if (siguiente?.tipo == "CORCHETE_APERTURA") {
                avanzar()
                NodoAST indice = expresion()
                coincidir("CORCHETE_CIERRE", "]")
                return new NodoAccesoArreglo(nombreVar, indice)
            }
            return new NodoVariable(nombreVar)
        } else if (actual.tipo == "CORCHETE_APERTURA") {
            return arregloLiteral()
        } else if (actual.tipo == "NUM_ENTERO" || actual.tipo == "NUM_DECIMAL") {
            Double val = Double.parseDouble(actual.valor)
            avanzar()
            return new NodoNumero(val)
        } else if (actual.tipo == "CADENA") {
            String val = actual.valor
            avanzar()
            return new NodoCadena(val)
        } else if (actual.tipo == "PR_TRUE") {
            avanzar()
            return new NodoBooleano(true)
        } else if (actual.tipo == "PR_FALSE") {
            avanzar()
            return new NodoBooleano(false)
        } else if (actual.tipo == "PR_NULL") {
            avanzar()
            return new NodoVacio()
        } else {
            imprimirError("se esperaba un numero variable cadena agrupacion o arreglo", actual)
            return new NodoVacio()
        }
    }

    // =========================================================
    // FUNCIONES DE CONTROL DE TOKENS
    // =========================================================

    Token obtenerTokenActual() {
        if (apuntador < listaTokens.size()) return listaTokens.get(apuntador)
        return null
    }

    Token verToken(int offset) {
        if (apuntador + offset < listaTokens.size()) return listaTokens.get(apuntador + offset)
        return null
    }

    void avanzar() { apuntador++ }

    void opcionalPuntoYComa() {
        Token actual = obtenerTokenActual()
        if (actual != null && actual.valor == ";") avanzar()
    }

    boolean esTipoDatoFuerte(Token t) {
        if (t == null) return false
        def tipos = ["PR_DEF", "PR_INT", "PR_FLOAT", "PR_DOUBLE", "PR_BOOLEAN", "PR_STRING", "PR_VOID", "PR_CHAR", "PR_BYTE", "PR_SHORT", "PR_LONG"]
        return tipos.contains(t.tipo)
    }

    void coincidir(String tipoEsperado, String valorEsperado) {
        Token actual = obtenerTokenActual()
        if (actual == null) {
            imprimirError("Fin de archivo inesperado. Se esperaba '" + (valorEsperado ?: tipoEsperado) + "'", null)
            return
        }

        boolean coincide = true
        if (actual.tipo != tipoEsperado) coincide = false
        if (valorEsperado != null && actual.valor != valorEsperado) coincide = false

        if (coincide) {
            avanzar()
        } else {
            String esperado = valorEsperado ?: tipoEsperado
            String sugerencia = ""

            // HEURÍSTICA DE INTELIGENCIA: Detectar si el usuario olvidó un operador matemático o lógico
            if (esperado == ")" && (actual.tipo == "IDENTIFICADOR" || actual.tipo == "NUM_ENTERO" || actual.tipo == "NUM_DECIMAL")) {
                sugerencia = "\n   -> [Sugerencia: ¿Olvidaste un operador como '+', '-', '<' o '==' entre tus variables?]"
            }
            // HEURÍSTICA: Si esperaba un punto y coma y encontró otra cosa
            else if (esperado == ";") {
                sugerencia = "\n   -> [Sugerencia: Revisa si te falta un ';' al final de la instruccion anterior.]"
            }

            imprimirError("Se esperaba '" + esperado + "' pero el compilador encontro '" + actual.valor + "'" + sugerencia, actual)
        }
    }

    void imprimirError(String mensaje, Token token) {
        hayErrores = true
        int lineaError = token != null ? token.linea : (!listaTokens.isEmpty() ? listaTokens.get(listaTokens.size() - 1).linea : 0)
        String tokenMalo = token != null ? token.valor : "FIN_DE_ARCHIVO"

        // Formateo de error estilo terminal profesional
        println "[X] ERROR SINTACTICO EN LINEA " + lineaError
        println "    Cerca del elemento: '" + tokenMalo + "'"
        println "    Detalle: " + mensaje
        println "--------------------------------------------------------"

        throw new Exception("Deteniendo compilacion.")
    }
}