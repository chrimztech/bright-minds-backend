package com.brightminds.school.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Without this, Jackson crashes trying to serialize an un-fetched Hibernate lazy-proxy
    // (any @ManyToOne(fetch = LAZY) field returned directly in a JSON response) with
    // "Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]".
    // FORCE_LAZY_LOADING is enabled because controllers here return entities directly and
    // callers expect nested associations (pupil names, class names, etc.) to be populated —
    // spring.jpa.open-in-view keeps the Hibernate session open through serialization, so the
    // lazy load succeeds. This trades a few extra per-request queries for correctness.
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
