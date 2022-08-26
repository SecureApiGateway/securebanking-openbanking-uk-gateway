/*
 * Copyright © 2020-2022 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.securebanking.uk.gateway.conversion.filter;

import com.adelean.inject.resources.junit.jupiter.GivenJsonResource;
import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;
import com.adelean.inject.resources.junit.jupiter.WithJacksonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgerock.securebanking.openbanking.uk.common.api.meta.share.IntentType;
import com.forgerock.securebanking.uk.gateway.conversion.converters.AccountAccessIntentConverter;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openig.handler.StaticResponseHandler;
import org.forgerock.openig.heap.EnvironmentHeap;
import org.forgerock.openig.heap.Name;
import org.forgerock.services.context.RootContext;
import org.junit.jupiter.api.Test;
import uk.org.openbanking.datamodel.account.OBReadConsentResponse1;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link IntentConverterFilter}
 */
@TestWithResources
public class IntentConverterFilterTest {

    @GivenTextResource("accountAccessIntent.json")
    String accountAccessIntent;

    @WithJacksonMapper // to specify which ObjectMapper is used to parse the JSON string
    ObjectMapper objectMapper = AccountAccessIntentConverter.genericConverterMapper();

    @GivenJsonResource("accountAccessIntent.json")
    OBReadConsentResponse1 obReadConsentResponse1Expected;

    @Test
    public void shouldConvertIntentToOBObject() throws Exception {
        // Given
        IntentConverterFilter filter = new IntentConverterFilter(IntentType.ACCOUNT_ACCESS_CONSENT, null);
        Request request = new Request();
        request.setEntity(accountAccessIntent);
        StaticResponseHandler handler = new StaticResponseHandler(Status.OK);
        // When
        Handler chain = Handlers.chainOf(handler, singletonList(filter));
        Response response = chain.handle(new RootContext(), request).get();
        // then
        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getEntity().getJson()).isEqualTo(obReadConsentResponse1Expected);
    }

    @Test
    public void shouldResponseWithError() throws Exception {
        // Given
        String entity = "Is not a json string";
        IntentConverterFilter filter = new IntentConverterFilter(IntentType.ACCOUNT_ACCESS_CONSENT, null);
        Request request = new Request();
        request.setEntity(entity);
        StaticResponseHandler handler = new StaticResponseHandler(Status.OK);
        // When
        Handler chain = Handlers.chainOf(handler, singletonList(filter));
        Response response = chain.handle(new RootContext(), request).get();
        // then
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getCause().getMessage()).contains(entity);
    }

    @Test
    public void shouldCreateHeaplet() throws Exception {
        // Given
        final JsonValue config = json(object(field("intentType", IntentType.ACCOUNT_ACCESS_CONSENT.toString())));
        EnvironmentHeap heap = mock(EnvironmentHeap.class);
        final IntentConverterFilter.Heaplet heaplet = new IntentConverterFilter.Heaplet();
        final IntentConverterFilter filter = (IntentConverterFilter) heaplet.create(Name.of("IntentIDMToOBObjectFilter"),
                config,
                heap);
        assertThat(filter).isNotNull();
    }

}
