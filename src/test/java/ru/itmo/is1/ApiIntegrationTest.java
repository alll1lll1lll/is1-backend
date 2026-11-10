package ru.itmo.is1;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest extends EmbeddedDatabaseTest {
    @Autowired
    MockMvc anonymous;

    @Autowired
    WebApplicationContext context;

    @Autowired
    ObjectMapper om;

    MockMvc mvc;

    @BeforeEach
    void authenticate() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String token = register(username, "secret123").get("accessToken").asText();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build();
    }

    private JsonNode register(String username, String password) throws Exception {
        return anonymousPost("/api/auth/register", credentials(username, password), 201);
    }

    private JsonNode anonymousPost(String url, String body, int expectedStatus) throws Exception {
        String res = anonymous.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(res);
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    @Test
    void authentication() throws Exception {
        anonymous.perform(get("/api/routes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        anonymous.perform(get("/api/routes").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());

        register("alice", "password1");
        JsonNode taken = anonymousPost("/api/auth/register", credentials("alice", "password2"), 409);
        assert "USERNAME_TAKEN".equals(taken.get("error").asText());

        JsonNode invalid = anonymousPost("/api/auth/register", credentials("b", "123"), 400);
        assert invalid.get("fieldErrors").has("username");
        assert invalid.get("fieldErrors").has("password");

        JsonNode wrong = anonymousPost("/api/auth/login", credentials("alice", "wrong-pass"), 401);
        assert "INVALID_CREDENTIALS".equals(wrong.get("error").asText());
        anonymousPost("/api/auth/login", credentials("nobody", "whatever"), 401);

        JsonNode login = anonymousPost("/api/auth/login", credentials("alice", "password1"), 200);
        assert login.hasNonNull("refreshToken");
        assert login.hasNonNull("accessTokenExpiresAt");
        anonymous.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
        anonymous.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotationAndReuseDetection() throws Exception {
        String firstRefresh = register("bob", "password1").get("refreshToken").asText();

        JsonNode rotated = anonymousPost("/api/auth/refresh", refreshBody(firstRefresh), 200);
        String secondRefresh = rotated.get("refreshToken").asText();
        assert !secondRefresh.equals(firstRefresh);
        assert "bob".equals(rotated.get("username").asText());
        anonymous.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rotated.get("accessToken").asText()))
                .andExpect(status().isOk());

        JsonNode reused = anonymousPost("/api/auth/refresh", refreshBody(firstRefresh), 401);
        assert "INVALID_REFRESH_TOKEN".equals(reused.get("error").asText());
        anonymousPost("/api/auth/refresh", refreshBody(secondRefresh), 401);

        String thirdRefresh = anonymousPost("/api/auth/login", credentials("bob", "password1"), 200)
                .get("refreshToken").asText();
        anonymous.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(thirdRefresh)))
                .andExpect(status().isNoContent());
        anonymousPost("/api/auth/refresh", refreshBody(thirdRefresh), 401);

        anonymousPost("/api/auth/refresh", refreshBody("garbage"), 401);
        JsonNode missing = anonymousPost("/api/auth/refresh", "{}", 400);
        assert missing.get("fieldErrors").has("refreshToken");
    }

    private static String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"%s\"}".formatted(refreshToken);
    }

    private JsonNode postJson(String url, String body, int expectedStatus) throws Exception {
        String res = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(res);
    }

    @Test
    void fullFlow() throws Exception {
        int a = postJson("/api/locations", "{\"x\":1,\"y\":1.5,\"name\":\"Alpha\"}", 201).get("id").asInt();
        int b = postJson("/api/locations", "{\"x\":10,\"y\":2,\"name\":\"Beta\"}", 201).get("id").asInt();

        postJson("/api/locations", "{\"x\":1,\"y\":1,\"name\":\"  \"}", 400);

        JsonNode r1 = postJson("/api/routes", """
                {"name":"Route one","coordinates":{"x":5,"y":6},"from":{"id":%d},"to":{"id":%d},
                 "distance":100,"rating":4.5}""".formatted(a, b), 201);
        int r1Id = r1.get("id").asInt();
        int coordId = r1.get("coordinates").get("id").asInt();

        postJson("/api/routes", """
                {"name":"Second road","coordinates":{"id":%d},"from":{"id":%d},"to":{"id":%d},
                 "distance":50,"rating":4.5}""".formatted(coordId, a, b), 201);
        postJson("/api/routes", """
                {"name":"Third","coordinates":{"x":0,"y":0},"from":{"x":99,"y":0,"name":"Gamma"},
                 "distance":7}""", 201);

        JsonNode err = postJson("/api/routes", """
                {"name":"","coordinates":{"x":1,"y":1},"distance":1,"rating":-1}""", 400);
        assert err.get("fieldErrors").has("name");
        assert err.get("fieldErrors").has("distance");
        assert err.get("fieldErrors").has("rating");

        mvc.perform(get("/api/routes/" + r1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from.name").value("Alpha"))
                .andExpect(jsonPath("$.creationDate").exists());

        mvc.perform(get("/api/routes?page=0&size=2&sort=name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Third"));
        mvc.perform(get("/api/routes?fromName=alp"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/routes?sort=to.name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/routes?sort=bogus,asc")).andExpect(status().isBadRequest());

        mvc.perform(put("/api/routes/" + r1Id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Route one upd","coordinates":{"id":%d},"from":{"id":%d},"to":null,
                         "distance":120,"rating":null}""".formatted(coordId, b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Route one upd"))
                .andExpect(jsonPath("$.to").doesNotExist());

        mvc.perform(get("/api/special/max-from"))
                .andExpect(jsonPath("$.from.name").value("Gamma"));
        mvc.perform(get("/api/special/count-by-rating?rating=4.5"))
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(get("/api/special/name-contains?substring=Route"))
                .andExpect(jsonPath("$", hasSize(1)));
        postJson("/api/special/between", """
                {"fromId":%d,"toId":%d,"name":"Long way","coordinates":{"x":1,"y":2},"distance":900}"""
                .formatted(a, b), 201);
        mvc.perform(get("/api/special/between?fromId=%d&toId=%d&mode=SHORTEST".formatted(a, b)))
                .andExpect(jsonPath("$.distance").value(50));
        mvc.perform(get("/api/special/between?fromId=%d&toId=%d&mode=LONGEST".formatted(a, b)))
                .andExpect(jsonPath("$.distance").value(900));

        mvc.perform(delete("/api/locations/" + a))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESOURCE_IN_USE"));
        mvc.perform(delete("/api/coordinates/" + coordId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESOURCE_IN_USE"));

        mvc.perform(delete("/api/routes/" + r1Id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/routes/" + r1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/routes/" + r1Id))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void errorResponses() throws Exception {
        JsonNode wrongType = postJson("/api/routes", """
                {"name":"x","coordinates":{"x":1,"y":1},"distance":"far"}""", 400);
        assert "VALIDATION_FAILED".equals(wrongType.get("error").asText());
        assert wrongType.get("fieldErrors").has("distance");

        JsonNode malformed = postJson("/api/routes", "{not json", 400);
        assert "MALFORMED_REQUEST".equals(malformed.get("error").asText());

        JsonNode badRef = postJson("/api/routes", """
                {"name":"x","coordinates":{"x":1},"distance":5}""", 400);
        assert badRef.get("fieldErrors").has("coordinates");

        mvc.perform(get("/api/special/count-by-rating?rating=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
        mvc.perform(get("/api/special/count-by-rating"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").value("required parameter"));
        mvc.perform(get("/api/special/between?fromId=1&toId=2&mode=WRONG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.mode").exists());
        mvc.perform(get("/api/special/between?fromId=999999&toId=999998"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/routes?sort=bogus,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        mvc.perform(patch("/api/routes/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }
}
