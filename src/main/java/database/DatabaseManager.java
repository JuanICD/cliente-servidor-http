package database;

import Hash.HashManager;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    Dotenv dotenv = Dotenv.load();
    // Configuración de la base de datos sacada las variables de entorno
    private static final String URL = Dotenv.load().get("DB_URL");
    private static final String USER = Dotenv.load().get("DB_USER");
    private static final String PASS = Dotenv.load().get("DB_PASSWORD");

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
     * @param user     Nombre del usuario.
     * @param password Contraseña en texto plano que será hasheada.
     * @return boolean true si se registró con éxito.
     */
    public boolean userRegister(String user, String password) {
        String query = "INSERT INTO user (username, password_hash) VALUES (?,?)";

        //Se genera el hash del password antes de insertarlo en la base de datos
        String hashedPassword = HashManager.hash(password);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            //Asignar los parametros a la consulta
            statement.setString(1, user);
            statement.setString(2, hashedPassword);

            //Ejecutar la consulta y comprobar si se ha insertado correctamente
            int affectedRows = statement.executeUpdate();
            LOGGER.info("Usuario: " + user + " registrado correctamente");

            //Si las filas afectadas son mayores a 0, se ha registrado correctamente y devuelve true
            return affectedRows > 0;


        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario: " + user);
            ex.printStackTrace();
            return false;
        }

    }

    /**
     * Actualiza la contraseña de un usuario en la base de datos.
     *
     * @param username    El nombre del usuario cuyo registro será actualizado.
     * @param newPassword La nueva contraseña en texto plano que será hasheada antes de ser almacenada.
     **/

    public boolean updateUser(String username, String newPassword) {
        String query = "UPDATE user SET password_hash = ? WHERE username = ?";
        String newPassHash = HashManager.hash(newPassword);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            //Asignar los parametros a la consulta
            statement.setString(1, newPassHash);
            statement.setString(2, username);

            int rows = statement.executeUpdate();
            //retorna true si afecta alguna fila
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    /**
     * Elimina un usuario de la base de datos según su nombre de usuario.
     *
     * @param username El nombre del usuario que se desea eliminar.
     * @return boolean true si el usuario fue eliminado exitosamente, false en caso contrario.
     */
    public boolean deleteUser(String username) {
        String query = "DELETE FROM user WHERE username = ?";

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            //Asignar los parametros a la consulta
            statement.setString(1, username);

            int rows = statement.executeUpdate();
            //retorna true si afecta alguna
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
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
        String query = "SELECT password_hash FROM user WHERE username = ?";
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
