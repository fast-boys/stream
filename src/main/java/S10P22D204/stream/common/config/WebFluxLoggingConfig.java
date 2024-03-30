package S10P22D204.stream.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebFluxLoggingConfig {

    @Bean
    public WebFilter loggingFilter() {
        return (exchange, chain) -> {
            // 요청 로깅
            System.out.println("Request: " + exchange.getRequest().getURI());

            return chain.filter(exchange).doOnEach(signal -> {
                // 응답 로깅
                if (!signal.isOnComplete()) {
                    System.out.println("Response: " + signal.get());
                }
            });
        };
    }
}
