package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.InvoiceEntity;
import org.jeecg.modules.business.entity.UserClient;
import org.jeecg.modules.business.mapper.ClientUserMapper;
import org.jeecg.modules.business.mapper.InvoiceEntityMapper;
import org.jeecg.modules.business.service.IUserClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class UserClientServiceImpl  extends ServiceImpl<ClientUserMapper, UserClient> implements IUserClientService {
    @Autowired
    private ClientUserMapper clientUserMapper;
    @Autowired
    private InvoiceEntityMapper invoiceEntityMapper;
    @Override
    public Client getClientByUserId(String userId) {
        return clientUserMapper.selectClientByUserId(userId);
    }

    @Override
    public List<Client> listClients() {
        List<Client> clients = clientUserMapper.listClients();
        for (Client c : clients) {
            c.setInvoiceEntityList(loadInvoiceEntities(c));
        }
        return clients;
    }

    @Override
    public Client getClientMinInfoByUserId(String userId) {
        Client client = clientUserMapper.getClientMinInfoByUserId(userId);
        if (client != null) {
            client.setInvoiceEntityList(loadInvoiceEntities(client));
        }
        return client;
    }

    private List<InvoiceEntity> loadInvoiceEntities(Client client) {
        List<InvoiceEntity> entities = invoiceEntityMapper.selectActiveByClientId(client.getId());
        if (!entities.isEmpty()) {
            entities.sort(Comparator.comparing(e -> !"1".equals(e.getIsDefault())));
            return entities;
        }
        return buildLegacyFallback(client);
    }

    private List<InvoiceEntity> buildLegacyFallback(Client client) {
        List<InvoiceEntity> fallback = new ArrayList<>();
        boolean hasCurrency = client.getCurrency() != null && !client.getCurrency().isEmpty();
        boolean hasInvoiceEntity = client.getInvoiceEntity() != null && !client.getInvoiceEntity().isEmpty();
        if (!hasCurrency && !hasInvoiceEntity) {
            return fallback;
        }
        InvoiceEntity entity = new InvoiceEntity();
        entity.setClientId(client.getId());
        entity.setInvoiceEntity(client.getInvoiceEntity());
        entity.setEmail(client.getEmail());
        entity.setPhone(client.getPhone());
        entity.setStreetNumber(client.getStreetNumber());
        entity.setStreetName(client.getStreetName());
        entity.setAdditionalAddress(client.getAdditionalAddress());
        entity.setPostcode(client.getPostcode());
        entity.setCity(client.getCity());
        entity.setCountry(client.getCountry());
        entity.setCurrency(client.getCurrency());
        entity.setCompanyIdType(client.getCompanyIdType());
        entity.setCompanyIdValue(client.getCompanyIdValue());
        entity.setIossNumber(client.getIossNumber());
        entity.setVatPercentage(client.getVatPercentage());
        entity.setIsDefault("1");
        entity.setActive("1");
        fallback.add(entity);
        return fallback;
    }

    @Override
    public List<Client> getClientsByCategory(String categoryName) {
        return clientUserMapper.getClientsByCategory(categoryName);
    }
}
