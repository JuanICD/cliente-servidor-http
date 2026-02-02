package Hash;

import database.DatabaseManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Logger;

/**
 * Clase para el manejo de la conversion del texto plano
 * de la contraseña a un nuevo hash
 */
public class HashManager {

    private static final String ALGORITHM = "SHA-256";

    //Logger para debuggear posibles errores
    private static final Logger LOGGER = Logger.getLogger(HashManager.class.getName());

    public static String hash(String password) {
        try {
            //Instanciamos el algoritmo de hash SHA-256
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);

            //Generamos el hash del password, convirtiendo el texto plano en bytes
            byte[]hash = md.digest(password.getBytes());

            //Convertimos el array de bytes resultante a formato Hexadecimal
            return HexFormat.of().formatHex(hash);

        }catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Error al generar hash: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error crítico: Algoritmo de hash no encontrado", e);
        }
    }
}
