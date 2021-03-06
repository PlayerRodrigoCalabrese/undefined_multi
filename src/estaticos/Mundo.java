package estaticos;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import login.LoginSocket;
import variables.Cuenta;
import variables.Servidor;

public class Mundo {
	public static final Map<Integer, Cuenta> Cuentas = new TreeMap<>();
	public static final Map<Integer, Servidor> Servidores = new ConcurrentHashMap<>();

	public static void crearMultiServer() {
		System.out.println("Creaci�n del MultiServidor: Creado!");
		System.out.print("Cargando las cuentas: ");
		GestorSQL.CARGAR_CUENTAS();
		System.out.println(Cuentas.size() + " cuentas cargadas");
	}

	public static synchronized void addCuenta(final Cuenta cuenta) {
		Cuentas.put(cuenta.getID(), cuenta);
	}

	public static Cuenta getCuenta(final int id) {
		return Cuentas.get(id);
	}

	public static byte getComunidadIdioma(String idioma) {
		return switch (idioma.toLowerCase()) {
			case "fr", "ch", "be", "lu" -> (byte) 0;
			case "uk", "ie", "gb" -> (byte) 1;
			case "xx" -> (byte) 2;
			case "de", "at", "li" -> (byte) 3;
			case "es", "ad", "ar", "ck", "mx" -> (byte) 4;
			case "ru" -> (byte) 5;
			case "pt", "br" -> (byte) 6;
			case "nl" -> (byte) 7;
			case "it" -> (byte) 9;
			case "jp" -> (byte) 10;
			default -> (byte) 99;
		};
	}

	public static String packetParaAH() {
		final StringBuilder str = new StringBuilder();
		for (final Servidor servidor : Servidores.values()) {
			if (str.length() > 0) {
				str.append("|");
			}
			str.append(servidor.getStringParaAH());
		}
		return str.toString();
	}

	public static void enviarPacketsAServidores(final LoginSocket cliente) {
		String packet = cliente.getPacketConexion();
		for (final Servidor servidor : Servidores.values()) {
			try {
				if (servidor.getConector() == null) {
					continue;
				}
				servidor.getConector().sendPacket(packet);
			} catch (final Exception ignored) {}
		}
	}

	public static String infoStatus() {
		StringBuilder str = new StringBuilder();
		for (final Servidor server : Servidores.values()) {
			try {
				String s = "$server_" + server.getID();
				str.append(s).append("_status = ").append(server.getEstado()).append(";\n");
				str.append(s).append("_onlines = ").append(server.getConectados()).append(";\n");
			} catch (final Exception ignored) {}
		}
		return str.toString();
	}
}
