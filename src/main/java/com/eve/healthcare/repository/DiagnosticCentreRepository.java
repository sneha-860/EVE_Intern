package com.eve.healthcare.repository;

import com.eve.healthcare.entity.DiagnosticCentre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticCentreRepository extends JpaRepository<DiagnosticCentre, Long> {
}
