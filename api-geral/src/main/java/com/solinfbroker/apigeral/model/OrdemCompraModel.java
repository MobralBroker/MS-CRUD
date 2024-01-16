package com.solinfbroker.apigeral.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@EqualsAndHashCode
@Table(name = "ordem_compra")
public class OrdemCompraModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "id_ativo", nullable = false)
    private Long idAtivo;

    private double valorCompra;

    private LocalDateTime dataCompra;

    private Integer quantidade; //TODO alterar banco

    private enumStatusOperacao status; //TODO alterar banco

    private LocalDateTime dataExecutada;

    @ManyToOne
    @JoinColumn(name = "id_cliente", referencedColumnName = "id", insertable = false, updatable = false)
    private ClienteModel cliente;

    @ManyToOne
    @JoinColumn(name = "id_ativo", referencedColumnName = "id", insertable = false, updatable = false)
    private AtivoModel ativo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "compra_venda", joinColumns =  @JoinColumn(name="id_compra"), inverseJoinColumns = @JoinColumn(name="id_venda"))
    private Set<OrdemVendaModel> ordemVendas;
}
