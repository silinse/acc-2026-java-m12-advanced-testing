package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    void findRecentAdoptionsByAnimalType_shouldFilterByAnimalTypeAndDate() {
        Animal dog1 = newAnimal("Rex", AnimalType.DOG);
        Animal dog2 = newAnimal("Max", AnimalType.DOG);
        Animal cat = newAnimal("Mia", AnimalType.CAT);

        entityManager.persist(dog1);
        entityManager.persist(dog2);
        entityManager.persist(cat);

        AdoptionRecord recentDog = new AdoptionRecord();
        recentDog.setAnimal(dog1);
        recentDog.setAdoptedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        recentDog.setAdopterEmail("john@example.com");

        AdoptionRecord oldDog = new AdoptionRecord();
        oldDog.setAnimal(dog2);
        oldDog.setAdoptedAt(LocalDateTime.of(2023, 1, 1, 0, 0));
        oldDog.setAdopterEmail("anna@example.com");

        AdoptionRecord recentCat = new AdoptionRecord();
        recentCat.setAnimal(cat);
        recentCat.setAdoptedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        recentCat.setAdopterEmail("trevor@example.com");

        entityManager.persist(recentDog);
        entityManager.persist(oldDog);
        entityManager.persist(recentCat);
        entityManager.flush();

        List<AdoptionRecord> result =
                adoptionRecordRepository.findRecentAdoptionsByAnimalType(
                        AnimalType.DOG,
                        LocalDateTime.of(2024, 1, 1, 0, 0));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAnimal()).isEqualTo(dog1);
    }

    private Animal newAnimal(String name, AnimalType type) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(AnimalStatus.ADOPTED);
        return animal;
    }
}
