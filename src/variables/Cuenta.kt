package variables

import estaticos.GestorSQL
import login.LoginSocket
import java.util.*

class Cuenta(val iD: Int, val nombre: String, apodo: String?) {
    var socket: LoginSocket? = null
    private val _personajes: MutableMap<Int, Int> = TreeMap()

    val tiempoAbono: Int
        get() = 0.coerceAtLeast((GestorSQL.GET_ABONO(nombre) - System.currentTimeMillis()).toInt())

    val actualizar: Byte
        get() = GestorSQL.GET_ACTUALIZAR(nombre)

    val contraseña: String
        get() = GestorSQL.GET_CONTRASEÑA_CUENTA(nombre)

    val apodo: String
        get() = GestorSQL.GET_APODO(nombre)

    val idioma: String
        get() = GestorSQL.GET_IDIOMA(nombre)

    val pregunta: String
        get() = GestorSQL.GET_PREGUNTA_SECRETA(nombre)

    val respuesta: String
        get() = GestorSQL.GET_RESPUESTA_SECRETA(nombre)

    val rango: Int
        get() = GestorSQL.GET_RANGO(nombre).toInt()

    val abono: Int
        get() = 0.coerceAtLeast((GestorSQL.GET_ABONO(nombre) - System.currentTimeMillis()).toInt())

    fun pararTimer() {
        try {
            socket!!.pararTimer()
        } catch (ignored: Exception) {
        }
    }

    val personajes: Map<Int, Int>
        get() = _personajes

    fun setPersonajes(servidor: Int, cantidad: Int) {
        _personajes[servidor] = cantidad
    }

    val stringPersonajes: String = ""
        get() = try {
            val str = StringBuilder()
            for ((key, value) in _personajes) {
                if (value < 1) {
                    continue
                }
                str.append("|").append(key).append(",").append(value)
            }
            str.toString()
        } catch (e: NullPointerException) {
            ""
        } catch (e: Exception) {
            field
        }

}