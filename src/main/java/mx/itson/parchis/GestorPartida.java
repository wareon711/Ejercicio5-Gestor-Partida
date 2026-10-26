package mx.itson.parchis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class GestorPartida extends JPanel implements MouseListener, ActionListener, OyentePartida {

    private static final String HOST_RIVAL = "localhost";
    private static final int PUERTO_RIVAL = 5000;
    private static final String URL_BD = "jdbc:mysql://localhost:3306/parchis?createDatabaseIfNotExist=true&connectTimeout=2000";
    private static final String USUARIO_BD = "root";
    private static final String CLAVE_BD = "";

    private static final int CELDAS = 19;

    private final Partida partida;
    private final RepositorioEstadisticas repositorioEstadisticas;
    private final NotificadorRed notificadorRed;

    private JLabel etiquetaTurno;
    private JLabel etiquetaDado;
    private JLabel etiquetaEstadisticas;
    private JButton botonDado;
    private JButton botonNueva;
    private JTextArea bitacora;

    private int tamCelda;
    private int origenX;
    private int origenY;

    public GestorPartida() {
        Jugador rojo = new Jugador("Jugador 1", ColorJugador.ROJO);
        Jugador azul = new Jugador("Jugador 2", ColorJugador.AZUL);
        this.repositorioEstadisticas = new RepositorioEstadisticasMySQL(URL_BD, USUARIO_BD, CLAVE_BD);
        this.notificadorRed = new NotificadorRedSocket(HOST_RIVAL, PUERTO_RIVAL);
        this.notificadorRed.setOyente(this);
        this.partida = new Partida(rojo, azul, notificadorRed, repositorioEstadisticas);
        this.partida.agregarOyente(this);

        setPreferredSize(new Dimension(620, 620));
        setBackground(new Color(0xF3F4EF));
        addMouseListener(this);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("espectador")) {
            NotificadorRedSocket.escucharJugadas(PUERTO_RIVAL);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            GestorPartida juego = new GestorPartida();
            JFrame ventana = new JFrame("Parchís");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(juego, BorderLayout.CENTER);
            ventana.add(juego.crearPanelLateral(), BorderLayout.EAST);
            ventana.pack();
            ventana.setLocationRelativeTo(null);

            juego.actualizarEstadisticasVisuales();
            juego.onMensajeBitacora("Empieza " + juego.partida.getJugadorEnTurno().getNombre()
                    + " (" + juego.partida.getJugadorEnTurno().getColor() + ").");
            juego.actualizarEtiquetas();
            ventana.setVisible(true);
        });
    }

    public JPanel crearPanelLateral() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(300, 620));

        JPanel arriba = new JPanel(new GridLayout(0, 1, 0, 8));
        JLabel titulo = new JLabel("Parchís");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        etiquetaTurno = new JLabel(" ");
        etiquetaTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        etiquetaDado = new JLabel("Dado: -");
        etiquetaDado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        botonDado = new JButton("Lanzar dado");
        botonDado.addActionListener(this);
        botonNueva = new JButton("Nueva partida");
        botonNueva.addActionListener(this);
        arriba.add(titulo);
        arriba.add(etiquetaTurno);
        arriba.add(etiquetaDado);
        arriba.add(botonDado);
        arriba.add(botonNueva);
        panel.add(arriba, BorderLayout.NORTH);

        bitacora = new JTextArea();
        bitacora.setEditable(false);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        bitacora.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(new JScrollPane(bitacora), BorderLayout.CENTER);

        etiquetaEstadisticas = new JLabel(" ");
        etiquetaEstadisticas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(etiquetaEstadisticas, BorderLayout.SOUTH);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == botonDado) {
            if (partida.isTerminada()) {
                JOptionPane.showMessageDialog(this, "La partida terminó. Empieza una nueva.");
                return;
            }
            if (partida.isYaTiro()) {
                JOptionPane.showMessageDialog(this, "Ya tiraste: elige una ficha.");
                return;
            }
            partida.lanzarDado();
        } else if (e.getSource() == botonNueva) {
            if (bitacora != null) {
                bitacora.setText("");
            }
            partida.nuevaPartida();
            actualizarEstadisticasVisuales();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (partida.isTerminada() || !partida.isYaTiro()) {
            return;
        }
        Jugador jugador = partida.getJugadorEnTurno();
        int[] fichas = jugador.getFichas();
        int radio = tamCelda / 2;

        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColorJugador(), i, fichas[i]);
            int dx = e.getX() - centro[0];
            int dy = e.getY() - centro[1];
            if (dx * dx + dy * dy <= radio * radio) {
                if (fichas[i] == Partida.META) {
                    JOptionPane.showMessageDialog(this, "Esa ficha ya llegó a la meta.");
                    return;
                }
                if (fichas[i] == -1 && partida.getDado() != 5) {
                    JOptionPane.showMessageDialog(this, "Para sacar una ficha de la base necesitas un 5.");
                    return;
                }
                if (fichas[i] >= 0 && fichas[i] + partida.getDado() > Partida.META) {
                    JOptionPane.showMessageDialog(this,
                            "Esa ficha necesita exactamente un " + (Partida.META - fichas[i]) + " para llegar a la meta.");
                    return;
                }
                partida.moverFicha(i);
                return;
            }
        }
    }

    @Override
    public void onMensajeBitacora(String mensaje) {
        if (bitacora != null) {
            bitacora.append(mensaje + "\n");
            bitacora.setCaretPosition(bitacora.getDocument().getLength());
        }
    }

    @Override
    public void onCambioEstado() {
        actualizarEtiquetas();
        repaint();
    }

    @Override
    public void onPartidaTerminada(Jugador ganador) {
        actualizarEstadisticasVisuales();
        JOptionPane.showMessageDialog(this, "¡Ganó " + ganador.getNombre() + " (" + ganador.getColor() + ")!");
    }

    private void actualizarEtiquetas() {
        if (etiquetaTurno == null || etiquetaDado == null || botonDado == null) {
            return;
        }
        Jugador jugador = partida.getJugadorEnTurno();
        if (partida.isTerminada()) {
            etiquetaTurno.setText("Partida terminada");
            etiquetaTurno.setForeground(Color.DARK_GRAY);
        } else {
            etiquetaTurno.setText("Turno: " + jugador.getNombre() + " (" + jugador.getColor() + ")");
            etiquetaTurno.setForeground(jugador.getColorJugador().getColorAwt());
        }
        int dado = partida.getDado();
        boolean yaTiro = partida.isYaTiro();
        etiquetaDado.setText("Dado: " + (dado == 0 ? "-" : String.valueOf(dado)) + (yaTiro ? " · elige una ficha" : ""));
        botonDado.setEnabled(!partida.isTerminada() && !yaTiro);
    }

    private void actualizarEstadisticasVisuales() {
        if (etiquetaEstadisticas != null) {
            etiquetaEstadisticas.setText(repositorioEstadisticas.obtenerResumen());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tamCelda = (Math.min(getWidth(), getHeight()) - 20) / CELDAS;
        origenX = (getWidth() - tamCelda * CELDAS) / 2;
        origenY = (getHeight() - tamCelda * CELDAS) / 2;

        dibujarBase(g2, 0, 0, ColorJugador.ROJO.getColorAwt());
        dibujarBase(g2, 0, 11, ColorJugador.AZUL.getColorAwt());
        dibujarBase(g2, 11, 0, new Color(0xD9DDD3));
        dibujarBase(g2, 11, 11, new Color(0xD9DDD3));

        for (int i = 0; i < Partida.TOTAL_CASILLAS; i++) {
            int[] celda = coordenadaCasilla(i);
            Color relleno = Color.WHITE;
            if (i == ColorJugador.ROJO.getCasillaSalida()) {
                relleno = ColorJugador.ROJO.getColorClaro();
            } else if (i == ColorJugador.AZUL.getCasillaSalida()) {
                relleno = ColorJugador.AZUL.getColorClaro();
            }
            pintarCelda(g2, celda[0], celda[1], relleno);
        }

        for (int i = 0; i < 7; i++) {
            int[] celdaRoja = coordenadaPasillo(ColorJugador.ROJO, i);
            int[] celdaAzul = coordenadaPasillo(ColorJugador.AZUL, i);
            pintarCelda(g2, celdaRoja[0], celdaRoja[1], ColorJugador.ROJO.getColorClaro());
            pintarCelda(g2, celdaAzul[0], celdaAzul[1], ColorJugador.AZUL.getColorClaro());
            pintarCelda(g2, 9, 17 - i, new Color(0xE7EAE3));
            pintarCelda(g2, 17 - i, 9, new Color(0xE7EAE3));
        }

        g2.setColor(new Color(0x14233A));
        g2.fillRect(origenX + 8 * tamCelda, origenY + 8 * tamCelda, 3 * tamCelda, 3 * tamCelda);
        g2.setColor(new Color(0xA9B6C6));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, tamCelda * 3 / 10)));
        FontMetrics medidas = g2.getFontMetrics();
        g2.drawString("META", origenX + 10 * tamCelda + tamCelda / 2 - medidas.stringWidth("META") / 2,
                origenY + 10 * tamCelda + tamCelda / 2 + medidas.getAscent() / 2 - 1);

        dibujarFichas(g2, partida.getJugadorRojo());
        dibujarFichas(g2, partida.getJugadorAzul());
    }

    private void dibujarBase(Graphics2D g2, int fila, int col, Color color) {
        g2.setColor(color);
        g2.fillRect(origenX + col * tamCelda, origenY + fila * tamCelda, 8 * tamCelda, 8 * tamCelda);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(origenX + (col + 1) * tamCelda, origenY + (fila + 1) * tamCelda,
                6 * tamCelda, 6 * tamCelda, tamCelda, tamCelda);
    }

    private void pintarCelda(Graphics2D g2, int fila, int col, Color relleno) {
        int x = origenX + col * tamCelda;
        int y = origenY + fila * tamCelda;
        g2.setColor(relleno);
        g2.fillRect(x, y, tamCelda, tamCelda);
        g2.setColor(new Color(0xB9C0B5));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, tamCelda, tamCelda);
    }

    private void dibujarFichas(Graphics2D g2, Jugador jugador) {
        int[] fichas = jugador.getFichas();
        boolean esSuTurno = (jugador.getColorJugador() == partida.getTurno());
        int radio = tamCelda * 2 / 5;

        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColorJugador(), i, fichas[i]);
            boolean movible = esSuTurno && partida.isYaTiro() && !partida.isTerminada()
                    && partida.esFichaMovible(jugador, i);

            if (movible) {
                g2.setColor(new Color(0x2A7A50));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(centro[0] - radio - 4, centro[1] - radio - 4, (radio + 4) * 2, (radio + 4) * 2);
            }

            g2.setColor(jugador.getColorJugador().getColorAwt());
            g2.fillOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, radio)));
            String num = String.valueOf(i + 1);
            FontMetrics medidas = g2.getFontMetrics();
            g2.drawString(num, centro[0] - medidas.stringWidth(num) / 2,
                    centro[1] + medidas.getAscent() / 2 - 1);
        }
    }

    private int[] coordenadaCasilla(int i) {
        if (i <= 7) return new int[] {8, i};
        if (i <= 15) return new int[] {15 - i, 8};
        if (i == 16) return new int[] {0, 9};
        if (i <= 24) return new int[] {i - 17, 10};
        if (i <= 32) return new int[] {8, i - 14};
        if (i == 33) return new int[] {9, 18};
        if (i <= 41) return new int[] {10, 52 - i};
        if (i <= 49) return new int[] {i - 31, 10};
        if (i == 50) return new int[] {18, 9};
        if (i <= 58) return new int[] {69 - i, 8};
        if (i <= 66) return new int[] {10, 66 - i};
        return new int[] {9, 0};
    }

    private int[] coordenadaPasillo(ColorJugador color, int i) {
        return color == ColorJugador.ROJO ? new int[] {9, 1 + i} : new int[] {1 + i, 9};
    }

    private int[] centroFicha(ColorJugador color, int num, int posicion) {
        double fila;
        double col;
        if (posicion == -1) {
            double[][] lugares = {{2.6, 2.6}, {2.6, 5.4}, {5.4, 2.6}, {5.4, 5.4}};
            int colBase = (color == ColorJugador.ROJO) ? 0 : 11;
            fila = lugares[num][0];
            col = colBase + lugares[num][1];
        } else if (posicion == Partida.META) {
            int[][] lugares = (color == ColorJugador.ROJO)
                    ? new int[][] {{9, 8}, {10, 8}, {10, 9}, {9, 9}}
                    : new int[][] {{8, 9}, {8, 10}, {9, 10}, {8, 8}};
            fila = lugares[num][0] + 0.5;
            col = lugares[num][1] + 0.5;
        } else {
            int[] celda = (posicion <= 66)
                    ? coordenadaCasilla((color.getCasillaSalida() + posicion) % Partida.TOTAL_CASILLAS)
                    : coordenadaPasillo(color, posicion - 67);
            double desp = (num - 1.5) * 0.12;
            fila = celda[0] + 0.5 + desp;
            col = celda[1] + 0.5 + desp;
        }
        return new int[] {(int) Math.round(origenX + col * tamCelda), (int) Math.round(origenY + fila * tamCelda)};
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
