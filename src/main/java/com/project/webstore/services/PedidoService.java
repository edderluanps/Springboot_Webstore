package com.project.webstore.services;

import com.project.webstore.domains.ItemPedido;
import com.project.webstore.domains.PagamentoComBoleto;
import com.project.webstore.domains.Pedidos;
import com.project.webstore.domains.enums.EstadoPagamento;
import com.project.webstore.repositories.ItemPedidoRepository;
import com.project.webstore.repositories.PagamentoRepository;
import com.project.webstore.repositories.PedidoRepository;
import com.project.webstore.services.exception.ObjectNotFoundException;
import java.util.Date;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {
    
    @Autowired
    private EmailService emailservice;

    @Autowired
    private PedidoRepository repo;

    @Autowired
    private BoletoService boletoService;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    @Autowired
    private ProdutosService produtoService;

    @Autowired
    private ClienteService clienteService;


    public Pedidos find(Integer id) {
        Optional<Pedidos> obj = repo.findById(id);
        return obj.orElseThrow(() -> new ObjectNotFoundException(
                "Objeto não encontrado! Id: " + id + ", Tipo: " + Pedidos.class.getName()));
    }

    public Pedidos insert(Pedidos obj) {
        obj.setId(null);
        obj.setInstante(new Date());
        obj.setCliente(clienteService.find(obj.getCliente().getId()));
        obj.getPagamento().setEstado(EstadoPagamento.PENDENTE);
        obj.getPagamento().setPedido(obj);
        
        if (obj.getPagamento() instanceof PagamentoComBoleto) {
            PagamentoComBoleto pagto = (PagamentoComBoleto) obj.getPagamento();
            boletoService.preencherPagamentoComBoleto(pagto, obj.getInstante());
        }
        
        obj = repo.save(obj);
        pagamentoRepository.save(obj.getPagamento());
        
        for (ItemPedido ip : obj.getItens()) {
            ip.setDesconto(0.0);
            ip.setProduto(produtoService.find(ip.getProduto().getId()));
            ip.setPreco(ip.getProduto().getPrice());
            ip.setPedido(obj);
        }
        
        itemPedidoRepository.saveAll(obj.getItens());
        
        emailservice.sendOrderConfirmationEmail(obj);
        
        return obj;
    
    }
}
