package fpt.capstone.edu360managementsystem.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvLoader {

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
