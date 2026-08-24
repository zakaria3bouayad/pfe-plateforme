package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByProjetIdOrderByDateEnvoiAsc(Long projetId);
}
