package software.amazon.neptune.onegraph.playground.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class Neptune1GPlaygroundServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(Neptune1GPlaygroundServerApplication.class, args);
    }
}
