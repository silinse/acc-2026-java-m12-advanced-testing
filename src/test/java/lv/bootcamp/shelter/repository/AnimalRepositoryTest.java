package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ticket: ANIMAL-3 (see README).
 * findByStatus_shouldReturnOnlyMatchingAnimals is already implemented —
 * mirror this pattern (and AdopterRepositoryTest's constraint test) to add
 * the second method yourself. You'll also need to create
 * AdoptionRecordRepositoryTest from scratch (see README).
 */
@DataJpaTest
class AnimalRepositoryTest {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private AnimalService animalService;

    @Test
    void findByStatus_shouldReturnOnlyMatchingAnimals() {
        entityManager.persist(newAnimal("Rex", AnimalType.DOG, AnimalStatus.AVAILABLE));
        entityManager.persist(newAnimal("Mia", AnimalType.CAT, AnimalStatus.ADOPTED));
        entityManager.flush();

        var available = animalRepository.findByStatus(AnimalStatus.AVAILABLE);

        assertThat(available).hasSize(1);
        assertThat(available.getFirst().getName()).isEqualTo("Rex");
    }

    @Test
    void save_shouldRejectDuplicatemichrochipId() {
        entityManager.persist(newAnimal("Rex", AnimalType.DOG, AnimalStatus.AVAILABLE, "MC123"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            animalRepository.save(newAnimal("Max", AnimalType.CAT, AnimalStatus.ADOPTED, "MC123"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }


    private Animal newAnimal(String name, AnimalType type, AnimalStatus status, String... microchipId) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(status);
        animal.setMicrochipId(microchipId.length > 0 ? microchipId[0] : null);
        return animal;
    }
}
