package com.brightminds.school.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    // Frontend forms send optional date fields (e.g. date of birth) as "" when left
    // blank rather than omitting them. Jackson's default LocalDate deserializer throws
    // on "", which previously surfaced as an opaque 500 on any create/update with a
    // blank optional date — treating blank as null makes those requests succeed like
    // any other omitted optional field.
    @Bean
    public SimpleModule blankAsNullLocalDateModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new StdDeserializer<>(LocalDate.class) {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getValueAsString();
                if (text == null || text.isBlank()) return null;
                return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });
        return module;
    }
}
