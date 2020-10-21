package estaticos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import login.LoginServer;
import sincronizador.SincronizadorServer;
import variables.Servidor;

public class MainMultiservidor {
    public static Calendar DATE_ERROR;
	public static Calendar DATE_ESTADISTICA;
	public static PrintStream LOG_ERRORES;
	public static PrintStream LOG_ESTADISTICAS;
	public static boolean ACTIVAR_CONSOLA = true;
	public static boolean ENCRIPTAR_IP;
	public static boolean PERMITIR_MULTICUENTA = true;
	public static boolean MOSTRAR_RECIBIDOS;
	public static boolean MOSTRAR_ENVIOS;
	public static boolean MOSTRAR_SINCRONIZACION;
	public static boolean MODO_DEBUG;
	public static boolean PARAM_ANTI_DDOS;
	public static boolean PARAM_MOSTRAR_IP;
	public static boolean PARAM_MOSTRAR_EXCEPTIONS;
	public static boolean ACTIVAR_FILA_ESPERA = true;
	public static boolean ACCESO_VIP;
	public static int PUERTO_MULTISERVIDOR = 444;
	public static int PUERTO_SINCRONIZADOR = 19999;
	// public static ArrayList<String> IP_SERVIDOR = new ArrayList<String>();// 25.91.217.194 -
	// 213.152.29.73
	public static String VERSION_CLIENTE = "1.29.1";
	public static String BD_HOST = "localhost";
	public static String BD_USUARIO = "root";
	public static String BD_PASS = "";
	public static String BD_CUENTAS = "";
	public static int MILISEGUNDOS_SIG_CONEXION = 500;
	public static int SEGUNDOS_INFO_STATUS = 60;
	public static int SEGUNDOS_ESTADISTICAS = 300;
	public static int SEGUNDOS_TRANSACCION_BD = 10;
	public static int SEGUNDOS_PARA_EXPULSAR = 0;
	public static int LIMITE_JUGADORES = 100;
	// public static int MAX_CONEXIONES_POR_IP = 8;
	public static byte MAX_CUENTAS_POR_IP = 8;
	public static byte MAX_CONEXION_POR_SEGUNDO = 10;

	public static void main(final String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(MainMultiservidor::cerrarServer));
		System.out.println("----------- UNDEFINED EMU - BASADO EN BUSTEMU ------------");
		System.out.println("Modificado por Player-xD - http://privatedofus.net");
		System.out.println("Gracias Elbusta");
		System.out.println("----------- UNDEFINED EMU - BASADO EN BUSTEMU ------------");
		System.out.println("\nCargando la configuraci�n");
		cargarConfiguracion();
		crearLogEstadisticas();
		crearLogErrores();
		if (GestorSQL.iniciarConexion()) {
			System.out.println("Conectado!");
		} else {
			escribirLog("CONEXION SQL INVALIDA!!");
			System.exit(1);
			return;
		}
		Mundo.crearMultiServer();
		new SincronizadorServer();
		new LoginServer();
		if (ACTIVAR_CONSOLA) {
			new Consola();
		}
		System.out.println("Esperando que los jugadores se conecten");
	}

	private static void crearLogErrores() {
		while (true) {
			try {
				DATE_ERROR = Calendar.getInstance();
				final String date = DATE_ERROR.get(Calendar.DAY_OF_MONTH) + "-" + (DATE_ERROR.get(Calendar.MONTH) + 1) + "-"
				+ DATE_ERROR.get(Calendar.YEAR);
				LOG_ERRORES = new PrintStream(new FileOutputStream("Logs_MultiServidor/Log_" + date + ".txt", true));
				LOG_ERRORES.println("---------- INICIO DEL MULTISERVIDOR ----------");
				LOG_ERRORES.flush();
				System.setErr(LOG_ERRORES);
				break;
			} catch (final IOException e) {
				new File("Logs_MultiServidor").mkdir();
			} catch (Exception e) {
				break;
			}
		}
	}

	private static void crearLogEstadisticas() {
		while (true) {
			try {
				DATE_ESTADISTICA = Calendar.getInstance();
				final String date = DATE_ESTADISTICA.get(Calendar.DAY_OF_MONTH) + "-"
				+ (DATE_ESTADISTICA.get(Calendar.MONTH) + 1) + "-" + DATE_ESTADISTICA.get(Calendar.YEAR);
				LOG_ESTADISTICAS = new PrintStream(new FileOutputStream("Estadisticas_Multiservidor/Conectados_" + date
				+ ".txt", true));
				LOG_ESTADISTICAS.flush();
				break;
			} catch (final IOException e) {
				new File("Estadisticas_Multiservidor").mkdir();
			} catch (Exception e) {
				break;
			}
		}
	}

	public static void cargarConfiguracion() {
		try {
            String ARCHIVO_CONFIG = "config_MultiServidor.txt";
            final BufferedReader config = new BufferedReader(new FileReader(ARCHIVO_CONFIG));
			String linea;
			ArrayList<String> comandos = new ArrayList<>();
			while ((linea = config.readLine()) != null) {
				try {
					final String param = linea.split("=")[0].trim();
					final String valor = linea.split("=")[1].trim();
					if (comandos.contains(param)) {
						System.out.println("En el " + ARCHIVO_CONFIG + " se repite el comando " + param);
						System.exit(1);
						return;
					} else {
						comandos.add(param);
					}
					// case "MAX_CONEXIONES_POR_IP" :
					// MAX_CONEXIONES_POR_IP = Integer.parseInt(valor);
					// break;
					switch (param.toUpperCase()) {
						case "ACTIVAR_CONSOLA" -> ACTIVAR_CONSOLA = valor.equalsIgnoreCase("true");
						case "MOSTRAR_ENVIADOS", "ENVIADOS" -> MOSTRAR_ENVIOS = valor.equalsIgnoreCase("true");
						case "SINCRONIZADOS", "MOSTRAR_SINCRONIZACION" -> MOSTRAR_SINCRONIZACION = valor.equalsIgnoreCase("true");
						case "MOSTRAR_RECIBIDOS", "RECIBIDOS" -> MOSTRAR_RECIBIDOS = valor.equalsIgnoreCase("true");
						case "MODO_DEBUG" -> MODO_DEBUG = valor.equalsIgnoreCase("true");
						case "PARAM_ANTI_DDOS" -> PARAM_ANTI_DDOS = valor.equalsIgnoreCase("true");
						case "PARAM_INFO_STATUS_PHP" -> PARAM_INFO_STATUS_PHP = valor.equalsIgnoreCase("true");
						case "PARAM_MOSTRAR_IP" -> PARAM_MOSTRAR_IP = valor.equalsIgnoreCase("true");
						case "PARAM_MOSTRAR_EXCEPTIONS" -> PARAM_MOSTRAR_EXCEPTIONS = valor.equalsIgnoreCase("true");
						case "ACTIVAR_FILA_ESPERA" -> ACTIVAR_FILA_ESPERA = valor.equalsIgnoreCase("true");
						case "ACCESO_VIP" -> ACCESO_VIP = valor.equalsIgnoreCase("true");
						case "ENCRIPTAR_IP" -> ENCRIPTAR_IP = valor.equalsIgnoreCase("true");
						case "VERSION_CLIENTE" -> VERSION_CLIENTE = valor;
						case "PUERTO_MULTISERVER", "PUERTO_MULTISERVIDOR" -> PUERTO_MULTISERVIDOR = Integer.parseInt(valor);
						case "PUERTO_SINCRONIZACION", "PUERTO_SINCRONIZADOR" -> PUERTO_SINCRONIZADOR = Integer.parseInt(valor);
						case "BD_HOST" -> BD_HOST = valor;
						case "BD_USER", "BD_USUARIO" -> BD_USUARIO = valor;
						case "BD_PASSWORD", "BD_CONTRASE�A", "BD_PASS" -> BD_PASS = valor;
						case "BD_ACCOUNTS", "BD_COMPTES", "BD_CUENTAS", "BD_LOGIN", "BD_REALM" -> BD_CUENTAS = valor;
						case "MAX_CUENTAS_POR_IP" -> MAX_CUENTAS_POR_IP = Byte.parseByte(valor);
						case "SEGUNDOS_PARA_EXPULSAR", "SEGUNDOS_PARA_BANEAR" -> SEGUNDOS_PARA_EXPULSAR = Integer.parseInt(valor);
						case "SEGUNDOS_TRANSACCION_BD", "TIEMPO_TRANSACCION_BD" -> SEGUNDOS_TRANSACCION_BD = Integer.parseInt(valor);
						case "SEGUNDOS_INFO_STATUS", "TIEMPO_INFO_STATUS" -> SEGUNDOS_INFO_STATUS = Integer.parseInt(valor);
						case "SEGUNDOS_ESTADISTICAS" -> SEGUNDOS_ESTADISTICAS = Integer.parseInt(valor);
						case "MAX_CONEXION_POR_SEGUNDO", "CONEXION_SEGUNDO" -> MAX_CONEXION_POR_SEGUNDO = (byte) (Byte.parseByte(valor) - 1);
						case "MILISEGUNDOS_SIG_CONEXION", "TIEMPO_SIG_CONEXION" -> MILISEGUNDOS_SIG_CONEXION = Integer.parseInt(valor);
						case "ENABLED_MULTIACCOUNT", "PERMITIR_MULTICUENTA" -> PERMITIR_MULTICUENTA = valor.equalsIgnoreCase("true");
						case "LIMITE_JUGADORES" -> LIMITE_JUGADORES = Short.parseShort(valor);
						case "CONFIG_SERVERS" -> {
							final String[] s = valor.split(";");
							for (final String sx : s) {
								try {
									final int id = Integer.parseInt(sx.split(",")[0]);
									final int puerto = Integer.parseInt(sx.split(",")[1]);
									Mundo.Servidores.put(id, new Servidor(id, puerto, Servidor.SERVIDOR_OFFLINE));
								} catch (final Exception e) {
									System.out.println("ERROR EN CONFIG_SERVER " + e.toString());
								}
							}
						}
					}
				} catch (Exception e) {}
			}
			config.close();
		} catch (final Exception e) {
			System.out.println(e.toString());
			System.out.println("Ficha de la configuraci�n no existe o ilegible");
		}
	}

	public static void escribirLog(final String str) {
		System.out.println(str);
		try {
			Calendar temp = Calendar.getInstance();
			if (temp.get(Calendar.DAY_OF_YEAR) != DATE_ERROR.get(Calendar.DAY_OF_YEAR)) {
				crearLogErrores();
			}
			final String hora = temp.get(Calendar.HOUR_OF_DAY) + ":" + temp.get(Calendar.MINUTE) + ":"
			+ temp.get(Calendar.SECOND);
			LOG_ERRORES.println("[" + hora + "]  " + str);
			LOG_ERRORES.flush();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void escribirEstadisticas() {
		try {
			Calendar temp = Calendar.getInstance();
			if (temp.get(Calendar.DAY_OF_YEAR) != DATE_ESTADISTICA.get(Calendar.DAY_OF_YEAR)) {
				crearLogEstadisticas();
			}
			final String hora = temp.get(Calendar.HOUR_OF_DAY) + ":" + temp.get(Calendar.MINUTE) + ":"
			+ temp.get(Calendar.SECOND);
			LOG_ESTADISTICAS.printf("%-8s", hora);
			for (final Servidor server : Mundo.Servidores.values()) {
				LOG_ESTADISTICAS.printf("\tSERVIDOR_%-4d(%1d):%3d", server.getID(), server.getEstado(), server.getConectados());
			}
			LOG_ESTADISTICAS.println();
			LOG_ESTADISTICAS.flush();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void cerrarServer() {
		System.out.println("SE ESTA INICIANDO EL CIERRE DEL SERVIDOR ...");
		GestorSQL.cerrarConexion();
		System.out.println("EL SERVIDOR SE CERRO EXITOSAMENTE");
	}
	private static boolean PARAM_INFO_STATUS_PHP = true;

	public static void infoStatus() {
		if (PARAM_INFO_STATUS_PHP) {
			try {
				BufferedWriter mod = new BufferedWriter(new FileWriter("info_status.php"));
				mod.write("<?php " + Mundo.infoStatus() + "?>");
				mod.flush();
				mod.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			for (final Servidor server : Mundo.Servidores.values()) {
				GestorSQL.UPDATE_INFO_STATUS(server.getID(), server.getConectados());
			}
		}
	}
}