package fpt.capstone.edu360managementsystem.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Environment variable loader utility.
 * Loads environment variables from .env file and sets them as system properties.
 * Used for local development configuration.
 *
 * @author 360edu
 * @version 1.0
 */
public class EnvLoader {

    /**
     * Loads environment variables from .env file into system properties.
     * Ignores missing .env file gracefully. Each entry from the .env file
     * is set as a system property for application access.
     */
    public static void load() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        System.out.println(".env loaded successfully");
    }
}
