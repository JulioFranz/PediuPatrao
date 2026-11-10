package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.Date;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    @Query(value = "{ 'dataHoraEntrada': { $gte: ?0, $lt: ?1 } }",
            sort = "{ 'dataHoraEntrada': -1, '_id': 1 }")
    List<Pedido> buscarNoPeriodo(Date inicio, Date fimExclusivo);
}
