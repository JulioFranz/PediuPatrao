package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.HorarioAtendimento;
import com.umc.pediupatrao.repository.HorarioRepository;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class HorarioService {
    private final HorarioRepository horarioRepository;
    private final List<String> dias = List.of("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo");

    public HorarioService(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    public List<HorarioAtendimento> listar() {
        List<HorarioAtendimento> horarios = new ArrayList<>();
        for (String dia : dias) {
            HorarioAtendimento horario = horarioRepository.findById(dia).orElse(null);
            if (horario == null) {
                horario = new HorarioAtendimento();
                horario.setFechado(true);
            }
            horario.setDia(dia);
            horarios.add(horario);
        }
        return horarios;
    }

    public void salvar(HorarioAtendimento horario) {
        if (!dias.contains(horario.getDia())) {
            throw new IllegalArgumentException("Selecione um dia da semana válido.");
        }
        if (horario.isFechado()) {
            horario.setAbertura(null);
            horario.setFechamento(null);
        } else {
            if (horario.getAbertura() == null || horario.getFechamento() == null) {
                throw new IllegalArgumentException("Informe a abertura e o fechamento.");
            }
            try {
                LocalTime abertura = LocalTime.parse(horario.getAbertura());
                LocalTime fechamento = LocalTime.parse(horario.getFechamento());
                if (abertura.equals(fechamento)) {
                    throw new IllegalArgumentException("A abertura e o fechamento precisam ser diferentes.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Informe horários válidos, como 18:00 e 23:30.");
            }
        }
        horarioRepository.save(horario);
    }
}
