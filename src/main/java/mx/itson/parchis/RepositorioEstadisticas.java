package mx.itson.parchis;

public interface RepositorioEstadisticas {

    void guardarPartida(ColorJugador ganador);

    String obtenerResumen();

    boolean estaDisponible();
}
