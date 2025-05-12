import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * Main class to run the Spring Boot application.
 * run with: mvn spring-boot:run
 */
@SpringBootApplication  
@ComponentScan(basePackages = {
        "PresentationLayer",
        "ApplicationLayer",
        "DomainLayer",
		"InfrastructureLayer"
})
public class Main {
	public static void main(String[] args){
		SpringApplication.run(Main.class, args);
	}

}
