package co.edu.eci.blueprints.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BlueprintsAPIControllerTest {

    private static final String BASE = "/api/v1/blueprints";

    @Autowired
    private MockMvc mvc;

    @Test
    void getAllReturnsSortedSeedData() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].author").value("jane"));
    }

    @Test
    void getByAuthorReturnsTotalPoints() throws Exception {
        mvc.perform(get(BASE).param("author", "juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author").value("juan"))
                .andExpect(jsonPath("$.data.totalPoints").value(7))
                .andExpect(jsonPath("$.data.blueprints", hasSize(2)))
                .andExpect(jsonPath("$.data.blueprints[0].name").value("plano-1"));

        mvc.perform(get(BASE + "/juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPoints").value(7));
    }

    @Test
    void getOneAndLegacyPath() throws Exception {
        mvc.perform(get(BASE + "/juan/plano-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author").value("juan"))
                .andExpect(jsonPath("$.data.name").value("plano-1"))
                .andExpect(jsonPath("$.data.points[0].x").value(50));

        mvc.perform(get("/api/blueprints/juan/plano-1"))
                .andExpect(status().isOk());
    }

    @Test
    void fullCrudFlow() throws Exception {
        String body = """
                {"author":"crud","name":"flow","points":[{"x":1,"y":2}]}
                """;

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.points", hasSize(1)));

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mvc.perform(put(BASE + "/crud/flow").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":[{\"x\":10,\"y\":20},{\"x\":30,\"y\":40}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points", hasSize(2)))
                .andExpect(jsonPath("$.data.points[1].y").value(40));

        mvc.perform(put(BASE + "/crud/flow/points").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":5,\"y\":6}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.data.points", hasSize(3)));

        mvc.perform(delete(BASE + "/crud/flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        mvc.perform(get(BASE + "/crud/flow"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void notFoundCases() throws Exception {
        mvc.perform(get(BASE + "/nobody/nothing")).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/nobody/nothing")).andExpect(status().isNotFound());
        mvc.perform(put(BASE + "/nobody/nothing").contentType(MediaType.APPLICATION_JSON)
                .content("{\"points\":[]}")).andExpect(status().isNotFound());
        mvc.perform(put(BASE + "/nobody/nothing/points").contentType(MediaType.APPLICATION_JSON)
                .content("{\"x\":1,\"y\":1}")).andExpect(status().isNotFound());
    }

    @Test
    void unknownRouteIs404Not500() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist/a/b/c/d"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void validationErrorsReturn400WithFieldMessages() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"bad name!\",\"name\":\"\",\"points\":[{\"x\":99999,\"y\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.author").exists())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data['points[0].x']").exists());

        mvc.perform(put(BASE + "/juan/plano-1/points").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":-20000,\"y\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.x").exists());

        mvc.perform(put(BASE + "/juan/plano-1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.points").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void corsAllowsFrontOrigin() throws Exception {
        mvc.perform(options(BASE)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
