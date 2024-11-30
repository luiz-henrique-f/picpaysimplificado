package com.picpaysimplificado.services;

import com.picpaysimplificado.domain.Transaction.Transaction;
import com.picpaysimplificado.domain.User.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        userService.validTransaction(sender, transaction.value());

        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
        if(!this.authorizeTransaction(sender, transaction.value())){
            throw new Exception("Transação não autorizada");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transação realizada com sucesso");
        this.notificationService.sendNotification(receiver, "Transação recebida com sucesso");

        return newTransaction;
    }

    public ResponseEntity<Boolean> getAuthorizationValue() {
        // Simula o JSON completo
        Map<String, Object> response = new HashMap<>();
        Map<String, Boolean> data = new HashMap<>();
        data.put("authorization", true);
        response.put("data", data);

        // Obtém o valor da chave "authorization"
        Boolean authorizationValue = data.get("authorization");

        // Retorna diretamente o valor como ResponseEntity
        return ResponseEntity.ok(authorizationValue);
    }

    public boolean authorizeTransaction(User sender, BigDecimal value){
        //ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
        ResponseEntity<Boolean> authorizationResponse = this.getAuthorizationValue();

        if(authorizationResponse.getBody()){
            return true;
        } else return false;
    }
}


