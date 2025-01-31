package org.opennms.horizon.inventory.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class GrpcServerManagerTest {

    public static final int TEST_PORT = 1313;

    private ServerInterceptor mockServerInterceptor;
    private Function<InetSocketAddress, NettyServerBuilder> mockNettyServerBuilderFactory;
    private NettyServerBuilder mockNettyServerBuilder;
    private Server mockServer;
    private BindableService mockProtoReflectionService;

    private GrpcServerManager target;

    @BeforeEach
    public void setUp() {
        mockServerInterceptor = Mockito.mock(ServerInterceptor.class);

        mockNettyServerBuilderFactory = Mockito.mock(Function.class);
        mockNettyServerBuilder = Mockito.mock(NettyServerBuilder.class);
        mockServer = Mockito.mock(Server.class);
        mockProtoReflectionService = Mockito.mock(BindableService.class);

        target = new GrpcServerManager(TEST_PORT, mockServerInterceptor);
        target.setNettyServerBuilderFactory(mockNettyServerBuilderFactory);
        target.setProtoReflectionService(mockProtoReflectionService);
    }

    @Test
    void testStartServer() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        BindableService mockBindableService = Mockito.mock(BindableService.class);
        prepareStartServerCommonInteractions(mockBindableService);

        //
        // Execute
        //
        target.startServer(mockBindableService);

        //
        // Verify the Results
        //
        Mockito.verify(mockServer).start();
    }

    @Test
    void testStartServerException() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        IOException testException = new IOException("x-io-exc-x");

        BindableService mockBindableService = Mockito.mock(BindableService.class);
        prepareStartServerCommonInteractions(mockBindableService);
        Mockito.when(mockServer.start()).thenThrow(testException);

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(GrpcServerManager.class)) {
            target.startServer(mockBindableService);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) -> (
                        Objects.equals("Couldn't start inventory gRPC server", logEvent.getMessage()) &&
                        (logEvent.getArguments().size() == 0) &&
                        ( logEvent.getThrowable().orElse(null) == testException)
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testStopServer() throws InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        BindableService mockBindableService = Mockito.mock(BindableService.class);
        prepareStartServerCommonInteractions(mockBindableService);
        Mockito.when(mockServer.isShutdown()).thenReturn(false);

        //
        // Execute
        //
        target.startServer(mockBindableService);
        target.stopServer();
        
        //
        // Verify the Results
        //
        Mockito.verify(mockServer).shutdown();
        Mockito.verify(mockServer).awaitTermination();
    }

    @Test
    void testStopServerAlreadyShutdown() throws InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        BindableService mockBindableService = Mockito.mock(BindableService.class);
        prepareStartServerCommonInteractions(mockBindableService);
        Mockito.when(mockServer.isShutdown()).thenReturn(true);

        //
        // Execute
        //
        target.startServer(mockBindableService);
        target.stopServer();

        //
        // Verify the Results
        //
        Mockito.verify(mockServer, Mockito.times(0)).shutdown();
        Mockito.verify(mockServer, Mockito.times(0)).awaitTermination();
    }

    @Test
    void testStopServerNull() throws InterruptedException {
        //
        // Execute
        //
        target.stopServer();

        //
        // Verify the Results
        //

        // No explicit verification needed; a lack of NPE is adequate
    }

//========================================
// Internals
//----------------------------------------

    private void prepareStartServerCommonInteractions(BindableService... testBindableServices) {
        Mockito.when(mockNettyServerBuilderFactory.apply(new InetSocketAddress(TEST_PORT))).thenReturn(mockNettyServerBuilder);
        Mockito.when(mockNettyServerBuilder.intercept(mockServerInterceptor)).thenReturn(mockNettyServerBuilder);

        Mockito.when(mockNettyServerBuilder.addService(mockProtoReflectionService)).thenReturn(mockNettyServerBuilder);
        for (BindableService oneBindableService : testBindableServices) {
            Mockito.when(mockNettyServerBuilder.addService(oneBindableService)).thenReturn(mockNettyServerBuilder);
        }

        Mockito.when(mockNettyServerBuilder.build()).thenReturn(mockServer);
    }
}
