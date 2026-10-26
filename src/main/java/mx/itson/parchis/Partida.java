package mx.itson.parchis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Partida {

    public static final int TOTAL_CASILLAS = 68;
    public static final int META = 74;

    private final Jugador jugadorRojo;
    private final Jugador jugadorAzul;
    private final NotificadorRed notificadorRed;
    private final RepositorioEstadisticas repositorioEstadisticas;
    private final List<OyentePartida> oyentes = new ArrayList<>();
    private final Random azar = new Random();

    private ColorJugador turno = ColorJugador.ROJO;
    private int dado = 0;
    private boolean yaTiro = false;
    private int seisSeguidos = 0;
    private boolean terminada = false;

    public Partida(Jugador jugadorRojo, Jugador jugadorAzul,
                   NotificadorRed notificadorRed,
                   RepositorioEstadisticas repositorioEstadisticas) {
        this.jugadorRojo = jugadorRojo;
        this.jugadorAzul = jugadorAzul;
        this.notificadorRed = notificadorRed;
        this.repositorioEstadisticas = repositorioEstadisticas;
    }

    public void agregarOyente(OyentePartida oyente) {
        if (oyente != null && !oyentes.contains(oyente)) {
            oyentes.add(oyente);
        }
    }

    private void avisarBitacora(String texto) {
        for (OyentePartida o : oyentes) {
            o.onMensajeBitacora(texto);
        }
    }

    private void avisarCambio() {
        for (OyentePartida o : oyentes) {
            o.onCambioEstado();
        }
    }

    private void avisarFin(Jugador ganador) {
        for (OyentePartida o : oyentes) {
            o.onPartidaTerminada(ganador);
        }
    }

    public Jugador getJugadorRojo() {
        return jugadorRojo;
    }

    public Jugador getJugadorAzul() {
        return jugadorAzul;
    }

    public Jugador getJugadorEnTurno() {
        return turno == ColorJugador.ROJO ? jugadorRojo : jugadorAzul;
    }

    public Jugador getRival() {
        return turno == ColorJugador.ROJO ? jugadorAzul : jugadorRojo;
    }

    public ColorJugador getTurno() {
        return turno;
    }

    public int getDado() {
        return dado;
    }

    public boolean isYaTiro() {
        return yaTiro;
    }

    public boolean isTerminada() {
        return terminada;
    }

    public RepositorioEstadisticas getRepositorioEstadisticas() {
        return repositorioEstadisticas;
    }

    public void lanzarDado() {
        if (terminada || yaTiro) {
            return;
        }
        procesarDado(azar.nextInt(6) + 1);
    }

    public void procesarDado(int valor) {
        Jugador jugador = getJugadorEnTurno();
        dado = valor;
        yaTiro = true;
        if (dado == 6) {
            seisSeguidos++;
        } else {
            seisSeguidos = 0;
        }

        avisarBitacora(jugador.getNombre() + " sacó " + dado + ".");
        notificadorRed.notificarDado(jugador.getColorJugador(), dado);

        if (!puedeMoverAlgunaFicha()) {
            avisarBitacora("No puede mover ninguna ficha.");
            yaTiro = false;
            if (dado == 6 && seisSeguidos < 3) {
                avisarBitacora("Sacó 6: vuelve a tirar.");
            } else {
                cambiarTurno();
            }
        }
        avisarCambio();
    }

    public boolean puedeMoverAlgunaFicha() {
        Jugador jugador = getJugadorEnTurno();
        int[] fichas = jugador.getFichas();
        for (int i = 0; i < fichas.length; i++) {
            if (esFichaMovible(jugador, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean esFichaMovible(Jugador jugador, int num) {
        if (num < 0 || num >= jugador.getFichas().length) {
            return false;
        }
        int pos = jugador.getFichas()[num];
        if (pos == -1 && dado == 5) {
            return true;
        }
        return (pos >= 0 && pos < META && pos + dado <= META);
    }

    public boolean moverFicha(int num) {
        if (terminada || !yaTiro) {
            return false;
        }
        Jugador jugador = getJugadorEnTurno();
        int[] fichas = jugador.getFichas();

        if (num < 0 || num >= fichas.length) {
            return false;
        }

        if (fichas[num] == -1) {
            if (dado != 5) {
                return false;
            }
            fichas[num] = 0;
            avisarBitacora(jugador.getNombre() + " sacó la ficha " + (num + 1) + " de su base.");
        } else {
            if (fichas[num] + dado > META) {
                return false;
            }
            fichas[num] = fichas[num] + dado;
            avisarBitacora(jugador.getNombre() + " avanzó " + dado
                    + (dado == 1 ? " casilla" : " casillas")
                    + " con la ficha " + (num + 1) + ".");
        }

        if (fichas[num] <= 66) {
            int casilla = (jugador.getCasillaSalida() + fichas[num]) % TOTAL_CASILLAS;
            Jugador rival = getRival();
            int[] fichasRival = rival.getFichas();
            for (int j = 0; j < fichasRival.length; j++) {
                if (fichasRival[j] >= 0 && fichasRival[j] <= 66) {
                    int casillaRival = (rival.getCasillaSalida() + fichasRival[j]) % TOTAL_CASILLAS;
                    if (casillaRival == casilla) {
                        fichasRival[j] = -1;
                        avisarBitacora("¡Capturó la ficha " + (j + 1) + " de " + rival.getNombre() + "! Regresa a su base.");
                        notificadorRed.notificarCaptura(rival.getColorJugador(), j);
                    }
                }
            }
        }

        notificadorRed.notificarMovimiento(jugador.getColorJugador(), num, fichas[num]);

        if (fichas[num] == META) {
            avisarBitacora("La ficha " + (num + 1) + " de " + jugador.getNombre() + " llegó a la meta.");
        }

        yaTiro = false;

        if (jugador.haGanado(META)) {
            terminarPartida(jugador);
            return true;
        }

        if (dado == 6 && seisSeguidos < 3) {
            avisarBitacora("Sacó 6: vuelve a tirar.");
        } else {
            cambiarTurno();
        }

        avisarCambio();
        return true;
    }

    private void cambiarTurno() {
        turno = turno.getSiguiente();
        seisSeguidos = 0;
        avisarBitacora("Turno de " + getJugadorEnTurno().getNombre() + " (" + turno.getNombre() + ").");
    }

    private void terminarPartida(Jugador ganador) {
        terminada = true;
        avisarBitacora("¡" + ganador.getNombre() + " ganó la partida!");
        notificadorRed.notificarFin(ganador.getColorJugador());
        repositorioEstadisticas.guardarPartida(ganador.getColorJugador());
        if (!repositorioEstadisticas.estaDisponible()) {
            avisarBitacora("Sin base de datos: no se guardaron las estadísticas.");
        }
        avisarCambio();
        avisarFin(ganador);
    }

    public void nuevaPartida() {
        jugadorRojo.reiniciar();
        jugadorAzul.reiniciar();
        turno = ColorJugador.ROJO;
        dado = 0;
        yaTiro = false;
        seisSeguidos = 0;
        terminada = false;
        avisarBitacora("Nueva partida. Empieza " + getJugadorEnTurno().getNombre() + ".");
        avisarCambio();
    }
}
