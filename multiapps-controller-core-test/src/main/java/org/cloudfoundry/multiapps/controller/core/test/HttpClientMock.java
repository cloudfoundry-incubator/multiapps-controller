package org.cloudfoundry.multiapps.controller.core.test;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@Value.Immutable
public abstract class HttpClientMock {

    public abstract List<HttpResponse> getResponses();

    @Nullable
    public abstract Throwable getException();

    @Nullable
    public abstract String getResponseHandlerReturnValue();

    @Value.Derived
    public CloseableHttpClient getMock() {
        try {
            CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
            if (getException() != null) {
                Mockito.when(httpClient.execute(ArgumentMatchers.any()))
                       .thenThrow(getException());
                return httpClient;
            }

            if (getResponses().isEmpty()) {
                throw new IllegalArgumentException("At least one response must be configured!");
            }

            Answer<HttpResponse> answer = createAnswer(getResponses());

            Mockito.when(httpClient.execute(ArgumentMatchers.any()))
                   .thenAnswer(answer);
            Mockito.when(httpClient.execute(ArgumentMatchers.<HttpHost> any(), ArgumentMatchers.any()))
                   .thenAnswer(answer);
            Mockito.when(httpClient.execute(ArgumentMatchers.<HttpHost> any(), ArgumentMatchers.any(),
                                            ArgumentMatchers.<HttpContext> any()))
                   .thenAnswer(answer);

            if (getResponseHandlerReturnValue() != null) {
                Mockito.when(httpClient.execute(ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                ArgumentMatchers.<ResponseHandler<Object>> any()))
                       .thenReturn(getResponseHandlerReturnValue());
            }
            return httpClient;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Answer<HttpResponse> createAnswer(List<HttpResponse> responses) {
        return new Answer<HttpResponse>() {

            private int invocations = -1;

            @Override
            public HttpResponse answer(InvocationOnMock invocation) {
                invocations++;
                if (invocations >= responses.size()) {
                    return responses.get(responses.size() - 1);
                }
                return responses.get(invocations);
            }

        };
    }

}
