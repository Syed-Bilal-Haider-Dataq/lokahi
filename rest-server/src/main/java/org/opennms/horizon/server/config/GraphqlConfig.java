/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */

package org.opennms.horizon.server.config;

import graphql.GraphQL;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.service.graphql.BffDataFetchExceptionHandler;
import org.opennms.horizon.server.service.graphql.DuplicateFieldValidation;
import org.opennms.horizon.server.service.graphql.ExecutionTimingInstrumentation;
import org.opennms.horizon.server.service.graphql.IntrospectionDisabler;
import org.opennms.horizon.server.service.graphql.MaxAliasOccurrenceValidation;
import org.opennms.horizon.server.service.graphql.MaxComplexityInstrumentation;
import org.opennms.horizon.server.service.graphql.MaxDepthInstrumentation;
import org.opennms.horizon.server.service.graphql.MaxDirectiveOccurrenceInstrumentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Provides fine-tuned configuration for GraphQL.
 *
 * @see io.leangen.graphql.spqr.spring.autoconfigure.BaseAutoConfiguration
 */
@Configuration
@Slf4j
public class GraphqlConfig {

    @Bean
    @Order(1)
    public Instrumentation timingInstrumentation() {
        return new ExecutionTimingInstrumentation();
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-query-depth:-1} > 1")
    @Order(2)
    public Instrumentation maxDepthInstrumentation(
        BffProperties properties
    ) {
        log.info("Limiting max query depth to {}", properties.getMaxQueryDepth());
        return new MaxDepthInstrumentation(properties.getMaxQueryDepth());
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-complexity:-1} > 1")
    @Order(3)
    public Instrumentation maxComplexityInstrumentation(
        BffProperties properties
    ) {
        log.info("Limiting max query complexity to {}", properties.getMaxComplexity());
        return new MaxComplexityInstrumentation(properties.getMaxComplexity());

    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-directive-occurrence:-1} > 0")
    @ConditionalOnBean
    @Order(4)
    public Instrumentation maxDirectiveOccurrenceInstrumentation(
        BffProperties properties
    ) {
        log.info("Limiting directive occurrences to {} or less", properties.getMaxDirectiveOccurrence());
        return new MaxDirectiveOccurrenceInstrumentation(
            properties.getMaxDirectiveOccurrence()
        );
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-alias-occurrence:-1} > 0")
    @Order(5)
    public Instrumentation maxAliasOccurrenceInstrumentation(
        BffProperties properties
    ) {
        log.info("Limiting alias occurrences to {} or less", properties.getMaxAliasOccurrence());
        return new FieldValidationInstrumentation(
            new MaxAliasOccurrenceValidation(properties.getMaxAliasOccurrence())
        );
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-field-occurrence:-1} > 0")
    @Order(6)
    public Instrumentation fieldDuplicationInstrumentation(
        BffProperties properties
    ) {
        log.info("Limiting field occurrences to {} or less", properties.getMaxFieldOccurrence());
        return new FieldValidationInstrumentation(
            new DuplicateFieldValidation(properties.getMaxFieldOccurrence())
        );
    }

    @Bean
    public DataFetcherExceptionHandler exceptionResolver() {
        return new BffDataFetchExceptionHandler();
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.introspection-enabled:false} == false")
    public IntrospectionDisabler introspectionDisabler() {
        log.info("Disabling introspection in graphql");
        return new IntrospectionDisabler();
    }

    @Bean
    public GraphQL graphQL(
        GraphQLSchema schema,
        List<Instrumentation> instrumentations,
        DataFetcherExceptionHandler exceptionResolver
    ) {
        if (log.isInfoEnabled()) {
            log.info("Configured Instrumentations: {}",
                instrumentations.stream().map(i -> i.getClass().getSimpleName()).toList()
            );
        }

        GraphQLRuntime.Builder builder = GraphQLRuntime.newGraphQL(schema);
        instrumentations.forEach(builder::instrumentation);
        builder.defaultDataFetcherExceptionHandler(exceptionResolver);

        return builder.build();
    }
}
