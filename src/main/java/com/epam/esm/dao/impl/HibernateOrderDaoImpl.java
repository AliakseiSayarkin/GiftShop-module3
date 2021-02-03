package com.epam.esm.dao.impl;

import com.epam.esm.dao.OrderDao;
import com.epam.esm.dao.request.OrderSearchCriteria;
import com.epam.esm.dao.service.PersistenceService;
import com.epam.esm.model.GiftCertificate;
import com.epam.esm.model.Order;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Repository
public class HibernateOrderDaoImpl implements OrderDao {

    @PersistenceContext
    private EntityManager entityManager;

    private static final boolean ACTIVE_ORDER = true;
    private static final boolean DELETED_ORDER = false;

    private final PersistenceService<Order> persistenceService;

    private static final String GET_ORDER_BY_USER_ID =
            "SELECT o FROM Order o WHERE o.user.id=:userId AND o.isActive=true ";
    private static final String GET_ALL_ORDERS = "SELECT o FROM Order o WHERE o.isActive=true ";
    private static final String GET_ORDER_COUNT = "SELECT count(o.id) FROM Order o WHERE o.isActive=true ";

    @Autowired
    public HibernateOrderDaoImpl(PersistenceService<Order> persistenceService) {
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    private void init() {
        persistenceService.setType(Order.class);
    }

    @Override
    public List<Order> getOrdersByUserId(
            int userId, OrderSearchCriteria searchCriteria, int page, int size) throws NoResultException {
        List<Order> orders = getNotAuditedOrdersByUserId(userId, searchCriteria, page, size);
        List<GiftCertificate> giftCertificateList = getFirstVersionOfOrders(orders);

        orders.forEach(o -> {
            List<GiftCertificate> temp = new ArrayList<>(o.getGiftCertificateList());
            giftCertificateList.forEach(c -> temp.replaceAll(t -> t.getId() == c.getId() ? c : t));
            o.setGiftCertificateList(new HashSet<>(temp));
        });

        return orders;
    }

    private List<Order> getNotAuditedOrdersByUserId(int id, OrderSearchCriteria searchCriteria, int page, int size) {
        String query = GET_ORDER_BY_USER_ID +
                "order by " + searchCriteria.getSortBy() + " " + searchCriteria.getSortType();
        TypedQuery<Order> typedQuery = entityManager.createQuery(query, Order.class);
        typedQuery.setParameter("userId", id);
        typedQuery.setFirstResult((page - 1) * size);
        typedQuery.setMaxResults(size);

        return typedQuery.getResultList();
    }

    private List<GiftCertificate> getFirstVersionOfOrders(List<Order> orders) {
        AuditQuery auditQuery = AuditReaderFactory.get(entityManager)
                .createQuery().forRevisionsOfEntity(GiftCertificate.class, true, true);
        auditQuery.add(AuditEntity.revisionNumber().minimize().computeAggregationInInstanceContext());

        orders.forEach(o -> {
            AuditDisjunction disjunction = AuditEntity.disjunction();
            o.getGiftCertificateList().forEach(c -> auditQuery.add(disjunction.add(AuditEntity.id().eq(c.getId()))));
        });

        return auditQuery.getResultList();
    }

    @Override
    public Order getOrderById(int orderId) {
        return persistenceService.getModelById(orderId);
    }

    @Override
    public List<Order> getAllOrdersByPage(OrderSearchCriteria searchCriteria, int page, int size) {
        return persistenceService.getAllModelsByPage(
                GET_ALL_ORDERS, page, size, searchCriteria.getSortType(), searchCriteria.getSortBy());
    }

    @Override
    public int getLastPage(int size) {
        return persistenceService.getLastPage(GET_ORDER_COUNT, size);
    }

    @Override
    public Order addOrder(Order order) {
        order.setActive(ACTIVE_ORDER);
        return persistenceService.add(order);
    }

    @Override
    public void deleteOrder(int orderId) {
        Order order = persistenceService.getModelById(orderId);
        if (order == null) {
            throw new NoResultException("Failed to find order to delete by id: " + orderId);
        }
        order.setActive(DELETED_ORDER);
        persistenceService.update(order);
    }
}
