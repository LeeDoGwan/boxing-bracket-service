package com.boxing.bracket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.boxing.bracket.common.config.JpaAuditingConfig;

@SpringBootApplication
@Import(JpaAuditingConfig.class)
public class BoxingBracketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoxingBracketServiceApplication.class, args);
    }
}
