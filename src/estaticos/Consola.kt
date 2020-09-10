package estaticos

import estaticos.GestorSQL.UPDATE_CUENTAS_LOG_CERO
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class Consola : Thread() {
    override fun run() {
        while (CONSOLA_ACTIVADA) {
            try {
                val b = BufferedReader(InputStreamReader(System.`in`))
                var linea = b.readLine()
                var str = ""
                try {
                    str = linea.substring(linea.indexOf(" ") + 1)
                    linea = linea.split(" ".toRegex()).toTypedArray()[0]
                } catch (ignored: Exception) {
                }
                leerComandos(linea, str)
            } catch (e: Exception) {
                println("Error al ingresar texto a la consola")
            }
        }
    }

    companion object {
        private var CONSOLA_ACTIVADA = true
        fun leerComandos(linea: String?, valor: String) {
            try {
                if (linea == null) {
                    return
                }
                when (linea.toUpperCase()) {
                    "ENVIADOS" -> MainMultiservidor.MOSTRAR_ENVIOS = valor.equals("true", ignoreCase = true)
                    "RECIBIDOS" -> MainMultiservidor.MOSTRAR_RECIBIDOS = valor.equals("true", ignoreCase = true)
                    "DEBUG" -> MainMultiservidor.MODO_DEBUG = valor.equals("true", ignoreCase = true)
                    "DESACTIVAR", "DESACTIVE", "DESACTIVER" -> {
                        CONSOLA_ACTIVADA = false
                        println("CONSOLA DESACTIVADA")
                    }
                    "DESLOGUEAR", "DESLOGUEADOS", "LOGS0" -> {
                        UPDATE_CUENTAS_LOG_CERO()
                        println("Logs 0")
                    }
                    "RELOG", "RECARGAR" -> {
                        MainMultiservidor.cargarConfiguracion()
                        println("Se recargo la config correctamente")
                    }
                    "EXIT", "RESET" -> exitProcess(0)
                    else -> {
                        println("Comando no existe")
                        return
                    }
                }
                println("Comando realizado: $linea -> $valor")
            } catch (e: Exception) {
                System.err.println("Ocurrio un error con el comando $linea")
            }
        }
    }

    init {
        this.isDaemon = true
        priority = 7
        start()
    }
}