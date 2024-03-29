package com.solinfbroker.apigeral.service;

import com.solinfbroker.apigeral.config.exceptions.RecursoNaoAceitoException;
import com.solinfbroker.apigeral.dtos.OperacaoDTO;
import com.solinfbroker.apigeral.dtos.OrdemDTO;
import com.solinfbroker.apigeral.model.*;
import com.solinfbroker.apigeral.repository.CarteiraRepository;
import com.solinfbroker.apigeral.repository.ClienteRepository;
import com.solinfbroker.apigeral.repository.OperacaoRepository;
import com.solinfbroker.apigeral.repository.OrdemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdemService {

    private final OrdemRepository ordemRepository;
    private final OperacaoRepository operacaoRepository;
    private final ClienteRepository clienteRepository;
    private final CarteiraRepository carteiraRepository;

    public List<Ordem> listarOrdem(){
        return ordemRepository.findAll();
    }

    public List<Ordem> listarOrdemAberta(){
        return ordemRepository.findByStatusOrdemAberta();
    }

    public Optional<Ordem> buscarOrdem(Long id){
        Optional<Ordem> ordem =ordemRepository.findById(id); // Busca da Ordem
        if(ordem.isPresent()){ // Verifica se possui a ordem com este ID
            if(ordem.get().getTipoOrdem().equals(enumTipoOrdem.ORDEM_VENDA)){ // Verica se é Venda ou compra
                List<OperacaoDTO> operacoes = operacaoRepository.findByIdVenda(ordem.get().getId())// Atribui as operações de venda que pertence a esta ordem

                        .stream()
                        .filter(Optional::isPresent)
                        .map(result ->
                                new OperacaoDTO(
                                        ((Long) result.get()[0]),
                                        (Integer) result.get()[1],
                                        ((Timestamp) result.get()[2]).toLocalDateTime(),
                                        enumStatus.valueOf((String) result.get()[3]),
                                        enumTipoOrdem.valueOf((String) result.get()[4]),
                                        (double) result.get()[5])

                        ).toList();
                ordem.get().setOperacoes(operacoes);
            }else{
                List<OperacaoDTO> operacoes = operacaoRepository.findByIdCompra(ordem.get().getId()) // Atribui as operações de compra que pertence a esta ordem
                        .stream()
                        .map(result -> new OperacaoDTO(
                                (Long) result[0],
                                (Integer)result[1],
                                ((Timestamp)result[2]).toLocalDateTime(),
                                enumStatus.valueOf((String)result[3]),
                                enumTipoOrdem.valueOf((String)result[4]),
                                (double) result[5]
                        )).toList();
                ordem.get().setOperacoes(operacoes);
            }

        }
        return ordem; //retorna ordem para o controller
    }

    public List<Ordem> buscarOrdemCliente(Long id){
        List<Ordem> ordemList =ordemRepository.findByIdClienteOrderByDataLancamentoDesc(id); // Busca da Ordem
        if(!ordemList.isEmpty()){ // Verifica se possui a ordem com este ID
            for ( Ordem ordem : ordemList){
                ordem.setSigla(ordem.getAtivo().getSigla());
                if(ordem.getTipoOrdem().equals(enumTipoOrdem.ORDEM_VENDA)){ // Verica se é Venda ou compra
                    List<OperacaoDTO> operacoes = operacaoRepository.findByIdVenda(ordem.getId())// Atribui as operações de venda que pertence a esta ordem

                            .stream()
                            .filter(Optional::isPresent)
                            .map(result ->
                                    new OperacaoDTO(
                                            ((Long) result.get()[0]),
                                            (Integer) result.get()[1],
                                            ((Timestamp) result.get()[2]).toLocalDateTime(),
                                            enumStatus.valueOf((String) result.get()[3]),
                                            enumTipoOrdem.valueOf((String) result.get()[4]),
                                            (double) result.get()[5])

                            ).toList();
                    ordem.setOperacoes(operacoes);
                }else{
                    List<OperacaoDTO> operacoes = operacaoRepository.findByIdCompra(ordem.getId()) // Atribui as operações de compra que pertence a esta ordem
                            .stream()
                            .map(result -> new OperacaoDTO(
                                    (Long) result[0],
                                    (Integer)result[1],
                                    ((Timestamp)result[2]).toLocalDateTime(),
                                    enumStatus.valueOf((String)result[3]),
                                    enumTipoOrdem.valueOf((String)result[4]),
                                    (double) result[5]
                            )).toList();
                    ordem.setOperacoes(operacoes);
                }

            }

        }
        return ordemList; //retorna ordem para o controller
    }
    public Ordem criarOrdem(OrdemDTO ordem){
        int indexControle = 0;
        Ordem ordemSalva = new Ordem();
        while(indexControle < 3 ) {

            Optional<ClienteModel> cliente = clienteRepository.findById(ordem.idCliente()); // busca o Cliente que esta fazendo a ordem
            double valorOrdem = ordem.quantidadeOrdem() * ordem.valorOrdem();
            ordemSalva = new Ordem();
            if (ordem.tipoOrdem().equals(enumTipoOrdem.ORDEM_COMPRA)) { // verifica o tipo de ordem como venda
                if (cliente.isPresent()) {
                    if (cliente.get().getSaldo() >= valorOrdem) { // verificar se o Cliente possui saldo para efetuar a compra
                        Ordem ordemSalvar = new Ordem();
                        BeanUtils.copyProperties(ordem, ordemSalvar);
                        ordemSalvar.setStatusOrdem(enumStatus.ABERTA);
                        ordemSalvar.setDataLancamento(LocalDateTime.now());
                        ordemSalvar.setQuantidadeAberto(ordemSalvar.getQuantidadeOrdem());
                        ordemSalvar.setValorClienteBloqueado(valorOrdem);


                        cliente.get().setValorBloqueado(cliente.get().getValorBloqueado() + valorOrdem); // Bloqueio do saldo do cliente
                        cliente.get().setSaldo(cliente.get().getSaldo() - valorOrdem);
                        try {
                            clienteRepository.save(cliente.get());
                            ordemSalva = ordemRepository.save(ordemSalvar);
                            indexControle = 5;
                        }catch (ObjectOptimisticLockingFailureException e){
                            indexControle = indexControle +1;
                        }

                    } else {
                        throw new RecursoNaoAceitoException("Criar Ordem", "saldo Insulficiente ou bloqueado!");
                    }
                }
            } else {
                if (cliente.isPresent()) {
                    Integer itensCarteira = carteiraRepository.buscarQuantideCarteira(cliente.get().getId(), ordem.idAtivo());
                    Integer itensCarteiraBloqueado = carteiraRepository.buscarQuantidadeBloqueadoCarteira(cliente.get().getId(), ordem.idAtivo());
                    if (itensCarteira == null) itensCarteira = 0;
                    if (itensCarteiraBloqueado == null) itensCarteiraBloqueado = 0;
//                boolean diferencaMaiorQueLimite = carteiraRepository.verificarDiferencaMaiorQueLimite(cliente.get().getId(), ordem.idAtivo(), ordem.quantidadeOrdem());

                    Integer quantDisponivel = itensCarteira - itensCarteiraBloqueado;

                    if (quantDisponivel == 0) {
                        throw new RecursoNaoAceitoException("Ordem", "Não possui Ações disponíveis suficiente!");
                    }

                    if (ordem.quantidadeOrdem() > quantDisponivel) {
                        throw new RecursoNaoAceitoException("Ordem", "Não possui Ações disponíveis suficiente!");
                    }

                    Ordem ordemSalvar = new Ordem();

                    if (quantDisponivel >= ordem.quantidadeOrdem()) {
                        CarteiraModel carteiraModel = new CarteiraModel();
                        carteiraModel.setIdCliente(cliente.get().getId());
                        carteiraModel.setIdAtivo(ordem.idAtivo());
                        carteiraModel.setQuantidade(0);
                        carteiraModel.setQuantidadeBloqueada(ordem.quantidadeOrdem());
                        carteiraModel.setDataCompra(LocalDateTime.now());


                        BeanUtils.copyProperties(ordem, ordemSalvar);
                        ordemSalvar.setStatusOrdem(enumStatus.ABERTA);
                        ordemSalvar.setDataLancamento(LocalDateTime.now());
                        ordemSalvar.setQuantidadeAberto(ordemSalvar.getQuantidadeOrdem());
                        ordemSalvar.setValorClienteBloqueado(valorOrdem);
                        try {
                            ordemSalva = ordemRepository.save(ordemSalvar);
                            carteiraRepository.save(carteiraModel);
                            indexControle = 5;
                        }catch (ObjectOptimisticLockingFailureException e){
                            indexControle = indexControle +1;
                        }
                    } else {
                        throw new RecursoNaoAceitoException("Ordem", "Não possui Ações disponíveis suficiente!");
                    }


                }
            }
        }
        if(indexControle ==3 && ordemSalva.getId() == null){
            throw new RecursoNaoAceitoException("Ordem", "Não Criar sua ordem, tente novamente!");
        }
        return ordemSalva;
    }

    public Ordem cancelarOrdem(Long id){
        Optional<Ordem> ordem = ordemRepository.findById(id);
        Ordem ordemSalva =new Ordem();
        if (ordem.isPresent()){
            if(ordem.get().getStatusOrdem().equals(enumStatus.EXECUTADA)){
                throw new RecursoNaoAceitoException("Cancelar Ordem ", "pois esta ordem já foi executada");
            }else{
                ordem.get().setStatusOrdem(enumStatus.CANCELADA);
                if(ordem.get().getTipoOrdem().equals(enumTipoOrdem.ORDEM_COMPRA)){
                    try {
                        double valorOrdem = ordem.get().getQuantidadeOrdem() * ordem.get().getValorOrdem();
                        Optional<ClienteModel> clienteModel = clienteRepository.findById(ordem.get().getIdCliente());
                        if(clienteModel.isPresent()){
                            clienteModel.get().setValorBloqueado(clienteModel.get().getValorBloqueado() - valorOrdem);
                            if(ordem.get().getTipoOrdem().equals(enumTipoOrdem.ORDEM_COMPRA)){
                                clienteModel.get().setSaldo(clienteModel.get().getSaldo() + valorOrdem);
                            }
                            clienteRepository.save(clienteModel.get());
                        }
                        ordemSalva = ordemRepository.save(ordem.get());
                    }catch (ObjectOptimisticLockingFailureException ex){ //Verifica se não esta sendo processada em outro lugar
                        throw new RecursoNaoAceitoException("Cancelar Ordem ", "pois esta ordem esta em conflido com processamento, tente novamente!");
                    }
                }else{
                    try {
                        List<CarteiraModel> carteiras = carteiraRepository.listarItensBloqueadoCarteira(ordem.get().getIdCliente(),ordem.get().getIdAtivo());
                        Integer qntRestante = ordem.get().getQuantidadeOrdem();
                        for(int i=0; i<carteiras.size();i++){
                            if(carteiras.get(i).getQuantidadeBloqueada() >0){
                                if(carteiras.get(i).getQuantidadeBloqueada() > qntRestante){
                                    carteiras.get(i).setQuantidadeBloqueada(carteiras.get(i).getQuantidadeBloqueada() - qntRestante);
                                    carteiraRepository.save(carteiras.get(i));
                                }else{
                                    carteiraRepository.delete(carteiras.get(i));
                                }
                                qntRestante = qntRestante - carteiras.get(i).getQuantidadeBloqueada();
                            }
                        }
                        ordemSalva = ordemRepository.save(ordem.get());
                    }catch (ObjectOptimisticLockingFailureException ex){ //Verifica se não esta sendo processada em outro lugar
                        throw new RecursoNaoAceitoException("Cancelar Ordem ", "pois esta ordem esta em conflido com processamento, tente novamente!");
                    }
                }
            }
        }
        return ordemSalva;
    }




}
