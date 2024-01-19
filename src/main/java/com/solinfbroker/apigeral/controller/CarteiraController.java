package com.solinfbroker.apigeral.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solinfbroker.apigeral.model.CarteiraModel;
import com.solinfbroker.apigeral.repository.CarteiraRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/carteira")
public class CarteiraController {


  private final CarteiraRepository carteiraRepository;

  @GetMapping
  private ResponseEntity<List<CarteiraModel>> listarCarteiras(){
      return ResponseEntity.ok(carteiraRepository.findAll());
  }
  
  @GetMapping("/cliente")
  private ResponseEntity<List<CarteiraModel>> listarCarteiraIdCliente(@RequestParam("idCliente") Long id) {
    return ResponseEntity.ok(carteiraRepository.findByClienteId(id));
  }
  
  @PostMapping
  private ResponseEntity<CarteiraModel> criarCarteira(@RequestBody CarteiraModel carteira){
      return ResponseEntity.ok(carteiraRepository.save(carteira));
  }
}