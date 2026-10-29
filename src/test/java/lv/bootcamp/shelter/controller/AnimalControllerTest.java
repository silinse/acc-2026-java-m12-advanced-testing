package lv.bootcamp.shelter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.bootcamp.shelter.config.SecurityConfig;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.service.AnimalNotFoundException;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ticket: ANIMAL-2 (see README).
 * findById_shouldReturn404WhenNotFound is already implemented as a worked
 * example (also see AdopterControllerTest for the same pattern) — add the
 * remaining tests yourself.
 * <p>
 *
 * @Import(SecurityConfig.class) is required: without it, this slice falls
 * back to Spring Boot's default "require auth for everything" security
 * instead of this project's actual (permissive, for these endpoints) rules.
 */
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnimalService animalService;

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        when(animalService.findById(99L)).thenThrow(new AnimalNotFoundException(99L));

        mockMvc.perform(get("/api/animals/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnCurrentAnimalsAsJson() throws Exception {
        AnimalResponse animal1 = new AnimalResponse(1L, "Rex", AnimalType.DOG, "Labrador",
                3, "Friendly", AnimalStatus.AVAILABLE);
        AnimalResponse animal2 = new AnimalResponse(2L, "Mia", AnimalType.CAT, "Orange",
                3, "Fast", AnimalStatus.AVAILABLE);
        AnimalResponse animal3 = new AnimalResponse(3L, "Max", AnimalType.DOG, "Labrador",
                3, "Slow", AnimalStatus.AVAILABLE);

        List<AnimalResponse> animals = new ArrayList<>(List.of(animal1, animal2, animal3));

        when(animalService.findAll()).thenReturn(animals);

        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Rex"))
                .andExpect(jsonPath("$[1].name").value("Mia"))
                .andExpect(jsonPath("$[2].name").value("Max"));
    }

    @Test
    void create_shouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AnimalCreateRequest(null, AnimalType.DOG, "Labrador", 3, "Friendly", "MC123"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(animalService);
    }
}
