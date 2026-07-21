class Environment {
    Map<String, Object> valores = [:]
    Environment padre // NUEVO: Para soportar funciones locales y variables globales

    Environment(Environment padre = null) {
        this.padre = padre
    }

    void asignar(String nombre, Object valor) {
        if (valores.containsKey(nombre)) {
            valores.put(nombre, valor)
        } else if (padre != null && padre.existe(nombre)) {
            padre.asignar(nombre, valor)
        } else {
            valores.put(nombre, valor)
        }
    }

    Object obtener(String nombre) {
        if (valores.containsKey(nombre)) {
            return valores.get(nombre)
        } else if (padre != null) {
            return padre.obtener(nombre)
        }
        throw new Exception("Error: Variable '" + nombre + "' no definida.")
    }

    boolean existe(String nombre) {
        if (valores.containsKey(nombre)) return true
        if (padre != null) return padre.existe(nombre)
        return false
    }
}

// ExcepcionReturn extraida a su propio archivo: scr/ExcepcionReturn.groovy