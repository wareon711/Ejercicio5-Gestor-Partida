package mx.itson.parchis;

public interface OyentePartida {

    void onMensajeBitacora(String mensaje);

    void onCambioEstado();

    void onPartidaTerminada(Jugador ganador);
}
