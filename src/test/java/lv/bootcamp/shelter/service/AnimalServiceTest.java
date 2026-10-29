package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ticket: ANIMAL-1 (see README).
 * Three tests are already implemented as a worked example (also see
 * AdopterServiceTest for the same pattern) — add the remaining tests yourself.
 */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void create_shouldSaveAnimalWithAvailableStatus() {
        Animal saved = new Animal();
        saved.setId(1L);
        saved.setName("Rex");
        saved.setType(AnimalType.DOG);
        saved.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.save(any(Animal.class))).thenReturn(saved);

        AnimalResponse response = animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.findById(99L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adopt_shouldThrowWhenAnimalAlreadyAdopted() {
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(1L, "Anna", "anna@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adopt_shouldChangeStatusAndTriggerAdoptionNotification() {
        Animal animal = new Animal();
        animal.setId(1L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);

        AdoptionRequest request = new AdoptionRequest(1L, "Anna", "anna@example.com");

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalRepository.save(any(Animal.class))).thenReturn(animal);

        AnimalResponse result = animalService.adopt(request);

        assertThat(result.status()).isEqualTo(AnimalStatus.ADOPTED);
        assertThat(animal.getStatus()).isEqualTo(AnimalStatus.ADOPTED);

        verify(notificationClient).sendAdoptionNotification(1L, "Rex", "anna@example.com");
    }

    @Test
    void reserveMultiple_shouldNotifyWithCorrectIds() {
        Animal animal1 = new Animal();
        animal1.setId(1L);
        animal1.setName("Rex");
        animal1.setStatus(AnimalStatus.AVAILABLE);

        Animal animal2 = new Animal();
        animal2.setId(2L);
        animal2.setName("Mia");
        animal2.setStatus(AnimalStatus.AVAILABLE);

        Animal animal3 = new Animal();
        animal3.setId(3L);
        animal3.setName("Max");
        animal3.setStatus(AnimalStatus.AVAILABLE);

        List<Animal> animalList = List.of(animal1, animal2, animal3);

        when(animalRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(animalList);
        when(animalRepository.save(any(Animal.class))).thenAnswer(returnsFirstArg());

        List<AnimalResponse> reservedAnimals = animalService.reserveMultiple(List.of(1L, 2L, 3L));

        assertThat(reservedAnimals)
                .extracting(AnimalResponse::id)
                .containsExactly(1L, 2L, 3L);

        assertThat(reservedAnimals)
                .extracting(AnimalResponse::status)
                .containsOnly(AnimalStatus.RESERVED);

        verify(notificationClient).sendBulkStatusNotification(List.of(1L, 2L, 3L), "RESERVED");
    }

    @Test
    void create_rejectAlreadyRegistered() {
        AnimalCreateRequest request = new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador",
                3, "Friendly", "MC123");

        when(animalRepository.existsByMicrochipId("MC123")).thenReturn(true);

        assertThatThrownBy(() -> animalService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Animal with microchip MC123 is already registered");

        verify(animalRepository, never()).save(any());
    }
}
