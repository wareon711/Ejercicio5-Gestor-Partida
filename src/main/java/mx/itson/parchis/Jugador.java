package mx.itson.parchis;

public class Jugador {

    private final String nombre;
    private final ColorJugador color;
    private final int[] fichas = {-1, -1, -1, -1};

    public Jugador(String nombre, ColorJugador color) {
        this.nombre = nombre;
        this.color = color;
    }

    public String getNombre() {
        return nombre;
    }

    public ColorJugador getColorJugador() {
        return color;
    }

    public String getColor() {
        return color.getNombre();
    }

    public int getCasillaSalida() {
        return color.getCasillaSalida();
    }

    public int[] getFichas() {
        return fichas;
    }

    public void reiniciar() {
        for (int i = 0; i < fichas.length; i++) {
            fichas[i] = -1;
        }
    }

    public boolean haGanado(int meta) {
        for (int pos : fichas) {
            if (pos != meta) {
                return false;
            }
        }
        return true;
    }
}
