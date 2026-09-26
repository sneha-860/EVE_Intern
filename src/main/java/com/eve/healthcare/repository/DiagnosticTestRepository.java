package com.eve.healthcare.repository;

import com.eve.healthcare.entity.DiagnosticTest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTest, Long> {
    List<DiagnosticTest> findByCentreId(Long centreId);
}
