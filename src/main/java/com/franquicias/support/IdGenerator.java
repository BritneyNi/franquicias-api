package com.franquicias.support;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Identifier generator used by the services.
 *
 * <p>It is a {@link Supplier} so tests can plug a deterministic implementation and assert on
 * exact identifiers.
 */
@FunctionalInterface
public interface IdGenerator extends Supplier<String> {

    /** Default production implementation: UUID v4 rendered as a string. */
    @Component
    class UuidGenerator implements IdGenerator {
        @Override
        public String get() {
            return UUID.randomUUID().toString();
        }
    }
}
