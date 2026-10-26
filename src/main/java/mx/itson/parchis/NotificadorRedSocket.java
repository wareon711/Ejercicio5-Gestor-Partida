package mx.itson.parchis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NotificadorRedSocket implements NotificadorRed {

    private final String hostRival;
    private final int puertoRival;
    private boolean avisoSinRed = false;
    private OyentePartida oyente;

    public NotificadorRedSocket(String hostRival, int puertoRival) {
        this.hostRival = hostRival;
        this.puertoRival = puertoRival;
    }

    @Override
    public void setOyente(OyentePartida oyente) {
        this.oyente = oyente;
    }

    private void enviar(String mensaje) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostRival, puertoRival), 300);
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            salida.println(mensaje);
        } catch (IOException e) {
            if (!avisoSinRed) {
                avisoSinRed = true;
                if (oyente != null) {
                    oyente.onMensajeBitacora("Sin conexión con " + hostRival + ":" + puertoRival
                            + ". Se sigue jugando en esta computadora.");
                }
            }
        }
    }

    @Override
    public void notificarDado(ColorJugador color, int dado) {
        enviar("DADO;" + color.getNombre() + ";" + dado);
    }

    @Override
    public void notificarMovimiento(ColorJugador color, int ficha, int casilla) {
        enviar("MOVIMIENTO;" + color.getNombre() + ";" + ficha + ";" + casilla);
    }

    @Override
    public void notificarCaptura(ColorJugador rival, int ficha) {
        enviar("CAPTURA;" + rival.getNombre() + ";" + ficha);
    }

    @Override
    public void notificarFin(ColorJugador ganador) {
        enviar("FIN;" + ganador.getNombre());
    }

    @Override
    public boolean estaConectado() {
        return !avisoSinRed;
    }

    public static void escucharJugadas(int puerto) {
        System.out.println("Esperando jugadas en el puerto " + puerto + "...");
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            while (true) {
                try (Socket cliente = servidor.accept();
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        System.out.println("Llegó: " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
        }
    }
}
