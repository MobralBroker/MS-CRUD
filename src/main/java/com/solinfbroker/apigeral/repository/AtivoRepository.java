package com.solinfbroker.apigeral.repository;

import com.solinfbroker.apigeral.model.AtivoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtivoRepository extends JpaRepository<AtivoModel,Long> {

  List<AtivoModel> findAllByOrderByAtualizacaoDesc();
  List<AtivoModel> findByEmpresaId(@Param("idEmpresa") Long idEmpresa);

  List<AtivoModel> findBysigla(@Param("sigla") String sigla);
}
