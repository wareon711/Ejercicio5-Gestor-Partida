package mx.itson.parchis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RepositorioEstadisticasMySQL implements RepositorioEstadisticas {

    private final String url;
    private final String usuario;
    private final String clave;
    private boolean disponible = false;

    public RepositorioEstadisticasMySQL(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        iniciarBD();
    }

    private void iniciarBD() {
        try (Connection con = DriverManager.getConnection(url, usuario, clave);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS estadisticas ("
                    + "color VARCHAR(10) PRIMARY KEY, "
                    + "jugadas INT NOT NULL DEFAULT 0, "
                    + "ganadas INT NOT NULL DEFAULT 0)");
            st.executeUpdate("INSERT IGNORE INTO estadisticas (color, jugadas, ganadas) VALUES ('Rojo', 0, 0), ('Azul', 0, 0)");
            disponible = true;
        } catch (SQLException e) {
            disponible = false;
            System.out.println("Sin base de datos: " + e.getMessage());
        }
    }

    @Override
    public boolean estaDisponible() {
        return disponible;
    }

    @Override
    public String obtenerResumen() {
        if (!disponible) {
            return "<html>Estadísticas: sin base de datos</html>";
        }
        String texto = "<html><b>Estadísticas</b>";
        try (Connection con = DriverManager.getConnection(url, usuario, clave);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT color, jugadas, ganadas FROM estadisticas ORDER BY color DESC")) {
            while (rs.next()) {
                texto += "<br>" + rs.getString("color") + ": " + rs.getInt("ganadas")
                        + " ganadas de " + rs.getInt("jugadas");
            }
        } catch (SQLException e) {
            texto += "<br>No se pudieron leer: " + e.getMessage();
        }
        return texto + "</html>";
    }

    @Override
    public void guardarPartida(ColorJugador ganador) {
        if (!disponible) {
            return;
        }
        String sql = "INSERT INTO estadisticas (color, jugadas, ganadas) VALUES (?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE jugadas = jugadas + 1, ganadas = ganadas + ?";
        try (Connection con = DriverManager.getConnection(url, usuario, clave);
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (ColorJugador col : ColorJugador.values()) {
                int gano = (col == ganador) ? 1 : 0;
                ps.setString(1, col.getNombre());
                ps.setInt(2, gano);
                ps.setInt(3, gano);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("No se guardaron las estadísticas: " + e.getMessage());
        }
    }
}
