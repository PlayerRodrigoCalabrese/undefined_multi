package login

import estaticos.*
import login.EnEspera.addEspera
import login.EnEspera.delEspera
import login.EnEspera.enEspera
import login.EnEspera.getIndexOf
import login.EnEspera.suTurno
import login.LoginServer.Companion.addCliente
import login.LoginServer.Companion.addEscogerServer
import login.LoginServer.Companion.borrarCliente
import login.LoginServer.Companion.borrarEscogerServer
import variables.Cuenta
import variables.Servidor
import java.awt.event.ActionListener
import java.io.BufferedInputStream
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.swing.Timer

class LoginSocket(socket: Socket?) : Runnable {
    private var _in: BufferedInputStream? = null
    var out: PrintWriter? = null
    private var _socket: Socket? = null
    private var _codigoLlave: String? = null
    var iP: String? = null
    private var _nombreCuenta: String? = null
    private var _tipoPacket = "CLIENTE"
    var cuenta: Cuenta? = null
        private set
    private var _timerBan: Timer? = null
    override fun run() {
        try {
            if (MainMultiservidor.SEGUNDOS_PARA_EXPULSAR > 0) {
                _timerBan = Timer(
                    MainMultiservidor.SEGUNDOS_PARA_EXPULSAR * 1000,
                    ActionListener {
                        // GestorSQL.INSERT_BAN_IP(_IP);
                        MainMultiservidor.escribirLog("LA IP $iP ESTA ATACANDO EL MULTISERVIDOR")
                        desconectar()
                    }
                )
            }
            GestorSalida.ENVIAR_XML_POLICY_FILE(out)
            _codigoLlave = GestorSalida.ENVIAR_HC_CODIGO_LLAVE(out)
            var c = -1
            var lenght = -1
            var index = 0
            var bytes = ByteArray(1)
            while (_in!!.read().also { c = it } != -1) {
                if (lenght == -1) {
                    lenght = _in!!.available()
                    bytes = ByteArray(lenght + 1)
                    index = 0
                }
                bytes[index++] = c.toByte()
                if (bytes.size == index) {
                    val tempPacket = String(bytes, StandardCharsets.UTF_8)
                    for (packet in tempPacket.split("[\u0000\n\r]".toRegex()).toTypedArray()) {
                        if (packet.isEmpty) {
                            continue
                        }
                        if (MainMultiservidor.MOSTRAR_RECIBIDOS) {
                            println("<<RECIBIR GENERAL:  $packet")
                        }
                        analizar_Packet_Real(packet)
                    }
                    lenght = -1
                }
            }
        } catch (e: Exception) {
            // e.printStackTrace();
        } finally {
            try {
                limpiarCuenta()
                desconectar()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun limpiarCuenta() {
        if (cuenta != null) {
            delEspera(cuenta)
            if (cuenta!!.socket === this) {
                cuenta!!.socket = null
            }
        }
    }

    val packetConexion: String
        get() = if (cuenta == null) {
            ""
        } else "A" + cuenta!!.iD + ";" + iP

    private fun desconectar() {
        try {
            borrarEscogerServer(this)
            borrarCliente(this)
            if (_socket != null && !_socket!!.isClosed) {
                _socket!!.close()
            }
            if (_in != null) {
                _in!!.close()
            }
            if (out != null) {
                out!!.close()
            }
            pararTimer()
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
    }

    fun pararTimer() {
        if (_timerBan != null) {
            try {
                _timerBan!!.stop()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun necesitaCompletarDatos(): Boolean {
        return cuenta!!.actualizar.toInt() == 3 || GestorSQL.GET_APELLIDO(_nombreCuenta).isEmpty
    }

    private fun analizar_Packet_Real(packet: String) {
        try {
            when (_tipoPacket) {
                "CLIENTE" -> {
                    if (packet.equals("<policy-file-request/>", ignoreCase = true)) {
                        // GestorSalida.ENVIAR_XML_POLICY_FILE(_out);
                        return
                    }
                    if (packet.length > 3 && packet.startsWith("##")) {
                        val param =
                            Encriptador.filtro(packet.substring(3)).split(";".toRegex()).toTypedArray()
                        GestorSalida.ENVIAR_HP_PASS_ENVIADA(
                            out, GestorSQL.GET_CONTRASEÑA_SI(
                                param[0], param[2], param[1],
                                param[3]
                            )
                        )
                    } else if (packet.length > 8 && packet.substring(0, 8).equals("BUSTOFUS", ignoreCase = true)) {
                        GestorSalida.ENVIAR_HR_RECUPERAR_CUENTA(
                            out, GestorSQL.GET_PREGUNTA_SECRETA(
                                Encriptador.filtro(
                                    packet
                                        .substring(8)
                                )
                            )
                        )
                    } else if (MainMultiservidor.VERSION_CLIENTE == "ANY" || packet.equals(
                            MainMultiservidor.VERSION_CLIENTE,
                            ignoreCase = true
                        )
                    ) {
                        _tipoPacket = "CUENTA"
                    } else {
                        GestorSalida.ENVIAR_AlEv_ERROR_VERSION_DEL_CLIENTE(out)
                        desconectar()
                        return
                    }
                }
                "CUENTA" -> {
                    _nombreCuenta = Encriptador.filtro(packet)
                    _tipoPacket = "PASSWORD"
                }
                "PASSWORD" -> {
                    if (packet.length < 3 || !packet.startsWith("#1")) {
                        GestorSalida.enviar(out, "ATE")
                        desconectar()
                        return
                    }
                    cuenta = Mundo.getCuenta(GestorSQL.GET_ID_CUENTA_NOMBRE(_nombreCuenta))
                    if (cuenta == null) {
                        GestorSQL.CARGAR_CUENTA_POR_NOMBRE(_nombreCuenta) // cuenta nueva
                        Thread.sleep(500)
                        cuenta = Mundo.getCuenta(GestorSQL.GET_ID_CUENTA_NOMBRE(_nombreCuenta))
                        if (cuenta == null) {
                            GestorSalida.ENVIAR_AlEp_CUENTA_NO_VALIDA(out)
                            desconectar()
                            return
                        }
                    }
                    val encriptada = _codigoLlave?.let {
                        Encriptador.encriptarContraseña(
                            it, GestorSQL
                                .GET_CONTRASEÑA_CUENTA(_nombreCuenta)
                        )
                    }
                    if (packet == encriptada) {
                        if (GestorSQL.ES_IP_BANEADA(iP)) {
                            GestorSalida.ENVIAR_AlEb_CUENTA_BANEADA_DEFINITIVO(out)
                            desconectar()
                            return
                        }
                        val tiempoBaneo = GestorSQL.GET_BANEADO(_nombreCuenta)
                        if (tiempoBaneo != 0L) {
                            when {
                                tiempoBaneo <= -1 -> {
                                    GestorSalida.ENVIAR_AlEb_CUENTA_BANEADA_DEFINITIVO(out)
                                    desconectar()
                                    return
                                }
                                tiempoBaneo > System.currentTimeMillis() -> {
                                    GestorSalida.ENVIAR_AlEk_CUENTA_BANEADA_TIEMPO(out, tiempoBaneo)
                                    desconectar()
                                    return
                                }
                                else -> {
                                    GestorSQL.SET_BANEADO(_nombreCuenta, 0)
                                }
                            }
                        }
                        // int conectados = 0;// GestorSQL.GET_CUENTAS_CONECTADAS_TOTAL();
                        // if (Bustemu.LIMITE_JUGADORES < conectados && _cuenta.getRango() == 0) {
                        // GestorSalida.ENVIAR_AlEw_MUCHOS_JUG_ONLINE(_out);
                        // return;
                        // }
                        if (MainMultiservidor.ACCESO_VIP) {
                            if (cuenta!!.tiempoAbono < 1) {
                                GestorSalida.ENVIAR_M1_MENSAJE_SERVER(
                                    out,
                                    "34",
                                    "Il faut être V.I.P. pour accéder à ce serveur",
                                    ""
                                )
                                GestorSalida.enviar(out, "ATE")
                                desconectar()
                                return
                            }
                        }
                        val cuentasPorIP = iP?.let { GestorSQL.GET_CUENTAS_CONECTADAS_IP(it) }
                        if (MainMultiservidor.PERMITIR_MULTICUENTA) {
                            if (cuentasPorIP != null) {
                                if (cuentasPorIP >= MainMultiservidor.MAX_CUENTAS_POR_IP) {
                                    GestorSalida.ENVIAR_M1_MENSAJE_SERVER(
                                        out, "34", cuentasPorIP.toString() + ";"
                                                + MainMultiservidor.MAX_CUENTAS_POR_IP, ""
                                    )
                                    GestorSalida.enviar(out, "ATE")
                                    desconectar()
                                    return
                                }
                            }
                        } else if (cuentasPorIP != null) {
                            if (cuentasPorIP > 0) {
                                GestorSalida.ENVIAR_M1_MENSAJE_SERVER(out, "34", "1;1", "")
                                GestorSalida.enviar(out, "ATE")
                                desconectar()
                                return
                            }
                        }
                        if (cuenta!!.socket != null && cuenta!!.socket !== this) {
                            GestorSalida.ENVIAR_AlEd_DESCONECTAR_CUENTA_CONECTADA(cuenta!!.socket!!.out)
                            cuenta!!.socket!!.desconectar()
                        }
                        _tipoPacket = "DEFAULT"
                        cuenta!!.socket = this
                        addEspera(cuenta!!)
                        if (_timerBan != null) {
                            _timerBan!!.start()
                        }
                        if (GestorSQL.GET_APODO(_nombreCuenta).isEmpty) {
                            var apodo = Encriptador.palabraAleatorio(12)
                            while (GestorSQL.GET_APODO_EXISTE(apodo)) {
                                apodo = Encriptador.palabraAleatorio(12)
                            }
                            GestorSQL.UPDATE_APODO(Encriptador.palabraAleatorio(12), cuenta!!.iD)
                        }
                        Mundo.enviarPacketsAServidores(this)
                        GestorSQL.UPDATE_ULTIMA_IP(iP, cuenta!!.iD)
                    } else {
                        GestorSalida.ENVIAR_AlEx_NOMBRE_O_PASS_INCORRECTA(out)
                        desconectar()
                        return
                    }
                }
                else -> if (cuenta == null) {
                    GestorSalida.ENVIAR_AlEp_CUENTA_NO_VALIDA(out)
                    desconectar()
                } else if (packet.startsWith("Af")) {
                    try {
                        if (_timerBan != null) {
                            _timerBan!!.restart()
                        }
                    } catch (ignored: Exception) {
                    }
                    if (MainMultiservidor.ACTIVAR_FILA_ESPERA) {
                        val pendiente = getIndexOf(cuenta)
                        when {
                            pendiente < MainMultiservidor.MAX_CONEXION_POR_SEGUNDO -> {
                                suTurno(cuenta!!, out)
                            }
                            pendiente == -1 -> {
                                desconectar()
                            }
                            else -> {
                                enEspera(pendiente, out)
                            }
                        }
                    } else {
                        suTurno(cuenta!!, out)
                    }
                } else if (packet.startsWith("Ax")) {
                    pararTimer()
                    if (cuenta!!.socket != null && cuenta!!.socket !== this) {
                        GestorSalida.ENVIAR_AlEd_DESCONECTAR_CUENTA_CONECTADA(cuenta!!.socket!!.out)
                        cuenta!!.socket!!.desconectar()
                        cuenta!!.socket = this
                    }
                    addEscogerServer(this)
                    GestorSalida.ENVIAR_AxK_TIEMPO_ABONADO_NRO_PJS(out, cuenta!!)
                    if (necesitaCompletarDatos()) {
                        GestorSalida.ENVIAR_HU_ACTUALIZAR_DATOS(out)
                    }
                } else if (packet.startsWith("AC")) { // cambiar password
                    pararTimer()
                    if (necesitaCompletarDatos()) {
                        GestorSalida.ENVIAR_HU_ACTUALIZAR_DATOS(out)
                    } else {
                        GestorSalida.ENVIAR_HN_CAMBIAR_PASSWORD(out, GestorSQL.GET_PREGUNTA_SECRETA(_nombreCuenta))
                    }
                } else if (packet.startsWith("AF")) { // confirmar nueva password
                    pararTimer()
                    if (necesitaCompletarDatos()) {
                        GestorSalida.ENVIAR_HU_ACTUALIZAR_DATOS(out)
                    } else {
                        try {
                            val param =
                                Encriptador.filtro(packet.substring(2)).split(";".toRegex()).toTypedArray()
                            GestorSQL.CAMBIAR_CONTRASEÑA(
                                cuenta!!.nombre,
                                param[1], param[2], param[3], cuenta!!.iD
                            )?.let {
                                GestorSalida.ENVIAR_HF_CONFIRMAR_NUEVA_PASSWORD(
                                    out, it
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            GestorSalida.ENVIAR_HF_CONFIRMAR_NUEVA_PASSWORD(out, "")
                        }
                    }
                } else if (packet.startsWith("UP")) { // actualizar datos
                    pararTimer()
                    if (necesitaCompletarDatos()) {
                        GestorSalida.ENVIAR_HU_ACTUALIZAR_DATOS(out)
                    } else {
                        GestorSalida.ENVIAR_BN_NADA(out)
                    }
                } else if (packet.startsWith("AX")) { // escoger server
                    pararTimer()
                    if (necesitaCompletarDatos()) {
                        GestorSalida.ENVIAR_HU_ACTUALIZAR_DATOS(out)
                    } else {
                        borrarEscogerServer(this)
                        LoginServer.Tiempos[iP!!] = System.currentTimeMillis()
                        val servidorID = packet.substring(2).toInt()
                        val servidor = Mundo.Servidores[servidorID]
                        if (servidor == null || servidor.estado == Servidor.SERVIDOR_OFFLINE) {
                            GestorSalida.ENVIAR_AlEn_CONEXION_NO_TERMINADA(out)
                            return
                        }
                        GestorSalida.ENVIAR_AXK_O_AYK_IP_SERVER(out, cuenta!!.iD, servidor.iP, servidor.puerto)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GestorSalida.ENVIAR_AlEp_CUENTA_NO_VALIDA(out)
            desconectar()
        }
    }

    init {
        try {
            _socket = socket
            iP = _socket!!.inetAddress.hostAddress
            _in = BufferedInputStream(_socket!!.getInputStream())
            out = PrintWriter(_socket!!.getOutputStream())
            addCliente(this)
            val _thread = Thread(this)
            _thread.isDaemon = true
            _thread.start()
        } catch (e: IOException) {
            desconectar()
        }
    }
}