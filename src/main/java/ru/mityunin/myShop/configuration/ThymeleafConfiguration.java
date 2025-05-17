package ru.mityunin.myShop.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class ThymeleafConfiguration {

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true); // Важно для реактивного режима
        return resolver;
    }

    @Bean
    public ISpringWebFluxTemplateEngine templateEngine() {
        SpringWebFluxTemplateEngine engine = new SpringWebFluxTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        return engine;
    }

    @Bean
    public ThymeleafReactiveViewResolver thymeleafReactiveViewResolver(
            ISpringWebFluxTemplateEngine templateEngine) {
        ThymeleafReactiveViewResolver resolver = new ThymeleafReactiveViewResolver();
        resolver.setTemplateEngine(templateEngine);
        resolver.setResponseMaxChunkSizeBytes(8192); // Оптимальный размер чанка
        resolver.setDefaultCharset(java.nio.charset.StandardCharsets.UTF_8);
        return resolver;
    }
}
