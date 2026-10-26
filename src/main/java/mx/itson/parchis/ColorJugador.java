package mx.itson.parchis;

import java.awt.Color;

public enum ColorJugador {
    ROJO("rojo", new Color(0xC23B2E), 1),
    AZUL("azul", new Color(0x2563A8), 18);

    private final String nombre;
    private final Color colorAwt;
    private final int casillaSalida;

    ColorJugador(String nombre, Color colorAwt, int casillaSalida) {
        this.nombre = nombre;
        this.colorAwt = colorAwt;
        this.casillaSalida = casillaSalida;
    }

    public String getNombre() {
        return nombre;
    }

    public Color getColorAwt() {
        return colorAwt;
    }

    public int getCasillaSalida() {
        return casillaSalida;
    }

    public Color getColorClaro() {
        return new Color(
                (colorAwt.getRed() + 2 * 255) / 3,
                (colorAwt.getGreen() + 2 * 255) / 3,
                (colorAwt.getBlue() + 2 * 255) / 3);
    }

    public ColorJugador getSiguiente() {
        return this == ROJO ? AZUL : ROJO;
    }

    public static ColorJugador desdeNombre(String texto) {
        if (texto == null) return null;
        for (ColorJugador c : values()) {
            if (c.nombre.equalsIgnoreCase(texto.trim())) {
                return c;
            }
        }
        return null;
    }
}
