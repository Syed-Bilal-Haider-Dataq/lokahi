package org.opennms.horizon.inventory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = InventoryApplication.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NodeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("inventory").withUsername("inventory")
        .withPassword("password").withExposedPorts(5432);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private NodeRepository nodeRepository;

    @LocalServerPort
    private Integer port;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @BeforeEach
    public void setup() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @AfterEach
    public void teardown() {
        nodeRepository.deleteAll();
    }

    @Test
    @Order(1)
    void testGetAll() throws Exception {
        String nodeLabel = "not here at all";
        postNodes(nodeLabel);
        postNodes(nodeLabel);

        ResponseEntity<List> response = this.testRestTemplate
            .getForEntity("http://localhost:" + port + "/inventory/nodes", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List body = response.getBody();
        assertEquals(2, body.size());

        assertEquals(nodeLabel, ((Map)body.get(0)).get("nodeLabel"));
        assertEquals(nodeLabel, ((Map)body.get(1)).get("nodeLabel"));
    }

    @Test
    @Order(2)
    void testPost() throws Exception {
        String nodeLabel = "not here";
        postNodes(nodeLabel);
    }

    private NodeDTO postNodes(String nodeLabel) {
        UUID tenant = new UUID(10, 12);
        NodeDTO ml = NodeDTO.newBuilder()
            .setNodeLabel(nodeLabel)
            .setTenantId(tenant.toString())
            .setCreateTime("2022-11-03T14:34:05.542488")
            .build();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<NodeDTO> request = new HttpEntity<>(ml, headers);

        ResponseEntity<NodeDTO> response = this.testRestTemplate
            .postForEntity("http://localhost:" + port + "/inventory/nodes", request, NodeDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        NodeDTO saved = response.getBody();
        assertEquals(tenant.toString(), saved.getTenantId());
        assertEquals(nodeLabel, saved.getNodeLabel());
        return saved;
    }

    @Test
    @Order(4)
    void testUpdate() throws Exception {
        String nodeLabel = "not here";
        NodeDTO ml = postNodes(nodeLabel);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<NodeDTO> request = new HttpEntity<>(ml, headers);

        // Update
        ResponseEntity<NodeDTO> putResponse = this.testRestTemplate
            .exchange("http://localhost:" + port + "/inventory/nodes", HttpMethod.PUT, request, NodeDTO.class);
        assertEquals(HttpStatus.OK, putResponse.getStatusCode());

        // Check there is one database entry
        ResponseEntity<List> response = this.testRestTemplate
            .getForEntity("http://localhost:" + port + "/inventory/nodes", List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List body = response.getBody();
        assertEquals(1, body.size());
    }

    @Test
    @Order(5)
    void testGet() throws Exception {
        String nodeLabel = "not here";
        NodeDTO ml = postNodes(nodeLabel);

        ResponseEntity<NodeDTO> response = this.testRestTemplate
            .getForEntity("http://localhost:" + port + "/inventory/nodes/" + ml.getId(), NodeDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        NodeDTO retrievedML = response.getBody();
        assertEquals(nodeLabel, retrievedML.getNodeLabel());
    }

    @Test
    @Order(6)
    void testGetNotFound() throws Exception {
        ResponseEntity<NodeDTO> response = this.testRestTemplate
            .getForEntity("http://localhost:" + port + "/inventory/nodes/1", NodeDTO.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(7)
    void testUpdateBadId() throws Exception {
        String nodeLabel = "not here";
        NodeDTO ml = postNodes(nodeLabel);

        NodeDTO bad = NodeDTO.newBuilder(ml)
                .setId(Long.MAX_VALUE)
                .build();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<NodeDTO> request = new HttpEntity<>(bad, headers);

        ResponseEntity<NodeDTO> response = this.testRestTemplate
            .exchange("http://localhost:" + port + "/inventory/nodes", HttpMethod.PUT, request, NodeDTO.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(8)
    void testPostExistingId() throws Exception {
        String nodeLabel = "not here";
        NodeDTO ml = postNodes(nodeLabel);

        NodeDTO bad = NodeDTO.newBuilder(ml)
            .setNodeLabel("something else")
            .build();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<NodeDTO> request = new HttpEntity<>(bad, headers);

        ResponseEntity<NodeDTO> response = this.testRestTemplate
            .postForEntity("http://localhost:" + port + "/inventory/nodes", request, NodeDTO.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}