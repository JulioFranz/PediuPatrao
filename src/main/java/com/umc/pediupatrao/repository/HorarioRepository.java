package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.HorarioAtendimento;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HorarioRepository extends MongoRepository<HorarioAtendimento, String> {
}
