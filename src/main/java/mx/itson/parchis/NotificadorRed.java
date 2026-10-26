package mx.itson.parchis;

public interface NotificadorRed {

    void notificarDado(ColorJugador color, int dado);

    void notificarMovimiento(ColorJugador color, int ficha, int casilla);

    void notificarCaptura(ColorJugador rival, int ficha);

    void notificarFin(ColorJugador ganador);

    boolean estaConectado();

    void setOyente(OyentePartida oyente);
}
