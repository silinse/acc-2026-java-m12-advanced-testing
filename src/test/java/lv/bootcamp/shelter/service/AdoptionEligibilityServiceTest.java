package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.*;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdoptionEligibilityServiceTest {

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService adoptionEligibilityService;

    @Test
    void evaluateAdoption_shouldRejectWhenAdopterNotFound() {
        when(adopterRepository.findById(1L))
                .thenReturn(Optional.empty());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ADOPTER_NOT_FOUND);
        assertThat(result.priorityScore()).isZero();

        verifyNoInteractions(animalRepository, notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectWhenAnimalNotFound() {
        Adopter adopter = new Adopter();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_FOUND);

        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectWhenAnimalNotAvailable() {
        Adopter adopter = new Adopter();
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.ADOPTED);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_AVAILABLE);

        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectUnderageAdopter() {
        Adopter adopter = new Adopter();
        Animal animal = newAnimal();
        adopter.setAge(17);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.UNDERAGE);

        verify(auditLogger).logRejection(1L, 2L, RejectionReason.UNDERAGE);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldAllowAdopterAtAge18() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        adopter.setAge(18);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        verify(notificationClient)
                .sendApprovalNotification("anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldRejectWhenPetLimitReachedOnSmallProperty() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        adopter.setCurrentPetCount(3);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);

        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldAllowTwoPetsOnSmallProperty() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        adopter.setCurrentPetCount(2);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        verify(auditLogger).logApproval(1L, 2L, 0);
        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluateAdoption_shouldRejectWhenPetLimitReachedOnLargeProperty() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(5);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);

        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldAllowThreePetsOnLargeProperty() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(3);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        verify(auditLogger).logApproval(1L, 2L, 9);
        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluateAdoption_shouldRejectBirdWitoutPermit() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        animal.setType(AnimalType.BIRD);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);

        verifyNoInteractions(notificationClient);
    }
    @Test

    void evaluateAdoption_shouldRejectRabbitWitoutPermit() {
        Adopter adopter = newAdopter();
        Animal animal = newAnimal();
        animal.setType(AnimalType.RABBIT);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);

        verifyNoInteractions(notificationClient);
    }


    @Test
    void evaluateAdoption_shouldCalculatePriorityScore() {
        Adopter adopter = newAdopter();
        adopter.setPreviousAdoptions(4);
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(2);

        Animal animal = newAnimal();
        animal.setAge(8);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(46);

        verify(auditLogger).logApproval(1L, 2L, 46);
        verify(notificationClient)
                .sendApprovalNotification("anna@example.com", "Rex");
    }

    private Adopter newAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setName("Anna");
        adopter.setEmail("anna@example.com");
        adopter.setAge(20);
        return adopter;
    }

    private Animal newAnimal() {
        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        return animal;
    }
}
