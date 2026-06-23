package com.swfactory.sdlc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal que inicializa el Backend de nuestra Factoría de Software Multi-Agente.
 */
@SpringBootApplication
@EnableAsync
public class SdlcAgenticSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdlcAgenticSystemApplication.class, args);
    }
}
