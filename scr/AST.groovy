// archivo scr/AST.groovy

abstract class NodoAST {
    abstract Object ejecutar(Environment env)
}

// nodo para guardar un bloque de codigo entero (el programa completo o el interior de un IF/FOR)
class NodoBloque extends NodoAST {
    List<NodoAST> instrucciones = []

    @Override
    Object ejecutar(Environment env) {
        Object ultimoValor = null
        for (NodoAST inst : instrucciones) {
            if (inst != null) {
                ultimoValor = inst.ejecutar(env)
            }
        }
        return ultimoValor
    }
}

// nodo para numeros
class NodoNumero extends NodoAST {
    Double valor

    NodoNumero(Double valor) {
        this.valor = valor
    }

    @Override
    Object ejecutar(Environment env) { return valor }
}

// nodo para textos
class NodoCadena extends NodoAST {
    String valor

    NodoCadena(String valor) {
        this.valor = valor.replace("\"", "") // quitamos las comillas
    }

    @Override
    Object ejecutar(Environment env) { return valor }
}

// nodo para imprimir
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

// nodo para sumas, restas, multiplicaciones, etc.
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
        
        throw new Exception("operador desconocido: " + operador)
    }
}