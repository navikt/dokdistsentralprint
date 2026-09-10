package no.nav.dokdistsentralprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ApplicationConfig.class)
public class Application {
	static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}