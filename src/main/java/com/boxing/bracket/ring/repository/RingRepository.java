package com.boxing.bracket.ring.repository;

import com.boxing.bracket.ring.domain.Ring;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RingRepository extends JpaRepository<Ring, Long> {
}
