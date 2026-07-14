package com.boxing.bracket.bout.repository;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class BoutOptimisticLockTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsStaleBoutUpdate() {
        Long boutId = persistBout();
        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            secondEntityManager.getTransaction().begin();
            Bout firstBout = firstEntityManager.find(Bout.class, boutId);
            Bout staleBout = secondEntityManager.find(Bout.class, boutId);

            firstBout.changeStatus(BoutStatus.READY);
            firstEntityManager.getTransaction().commit();

            staleBout.changeStatus(BoutStatus.IN_PROGRESS);
            assertThatThrownBy(() -> secondEntityManager.getTransaction().commit())
                    .isInstanceOfAny(OptimisticLockException.class, RollbackException.class);
        } finally {
            rollbackIfActive(firstEntityManager);
            rollbackIfActive(secondEntityManager);
            firstEntityManager.close();
            secondEntityManager.close();
            deleteBout(boutId);
        }
    }

    private Long persistBout() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Bout bout = Bout.builder()
                    .tournamentId(1L)
                    .ringId(1L)
                    .boutNumber(1)
                    .redAthleteId(10L)
                    .blueAthleteId(11L)
                    .status(BoutStatus.SCHEDULED)
                    .build();
            entityManager.persist(bout);
            entityManager.getTransaction().commit();
            return bout.getId();
        } finally {
            rollbackIfActive(entityManager);
            entityManager.close();
        }
    }

    private void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private void deleteBout(Long boutId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Bout bout = entityManager.find(Bout.class, boutId);
            if (bout != null) {
                entityManager.remove(bout);
            }
            entityManager.getTransaction().commit();
        } finally {
            rollbackIfActive(entityManager);
            entityManager.close();
        }
    }
}
