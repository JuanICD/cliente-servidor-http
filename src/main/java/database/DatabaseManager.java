package database;

import Hash.HashManager;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    // Configuración de la base de datos sacada las variables de entorno
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASS");

    //Logger para debuggear posibles errores
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /**
     * Establece la conexion con la base de datos
     *
     * @return
     */
    private Connection connection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * Registra un nuevo usuario aplicando Hash a la contraseña.
     *
     * @param username Nombre del usuario.
     * @param password Contraseña en texto plano que será hasheada.
     * @return boolean true si se registró con éxito.
     */
    public synchronized boolean userRegiter(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?,?)";

        //Se genera el hash del password antes de insertarlo en la base de datos
        String hashedPassword = HashManager.hash(password);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            //Asignar los parametros a la consulta
            statement.setString(1, username);
            statement.setString(2, hashedPassword);

            //Ejecutar la consulta y comprobar si se ha insertado correctamente
            int affectedRows = statement.executeUpdate();
            LOGGER.info("Usuario: " + username + " registrado correctamente");

            //Si las filas afectadas son mayores a 0, se ha registrado correctamente y devuelve true
            return affectedRows > 0;


        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario: " + username);
            return false;
        }

    }

    /**
     * Válida las credenciales de un usuario comparando el nombre de usuario y la contraseña
     * proporcionados con la contraseña cifrada almacenada en la base de datos.
     *
     * @param username El nombre de usuario proporcionado por el usuario.
     * @param password El password proporcionado por el usuario.
     * @return boolean true si las credenciales coinciden, false si no.
     */
    public synchronized boolean validateUser(String username, String password) {
        //Consulta para buscar la password del usuario en la base de datos por su username
        String query = "SELECT password_hash FROM users WHERE username = ?";
        //Hasheamos la contraseña antes de compararla con la de la base de datos, para comparar por hash
        String hashedPassword = HashManager.hash(password);

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                //Extraemos la password de la base de datos por el nombre de la columna
                String dbPassword = rs.getString("password_hash");
                //La comparamos con la hasheada del password introducido por el usuario
                return hashedPassword.equals(dbPassword);
            }
            //Si llega aqui las contraseñas no coinciden
            LOGGER.warning("Intento de acceso fallido para el usuario: " + username);
            return false;

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al validar usuario: " + username);
            return false;
        }

    }

}
