package variables

import sincronizador.SincronizadorSocket

class Servidor(val iD: Int, val puerto: Int, var estado: Int) {
    var iP = "127.0.0.1"
    var conector: SincronizadorSocket? = null
    var conectados = 0
    private var _prioridad = 0

    fun setPrioridad(prioridad: Int) {
        _prioridad = prioridad
    }

    val stringParaAH: String
        get() {
            val _puedeLoguear = true
            return iD.toString() + ";" + estado + ";" + _prioridad + ";" + if (_puedeLoguear) 1 else 0
        }

    companion object {
        const val SERVIDOR_OFFLINE = 0
        const val SERVIDOR_ONLINE = 1
        const val SERVIDOR_SAVING = 2
    }
}