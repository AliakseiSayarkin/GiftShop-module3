package com.epam.esm.web.hateoas;

import com.epam.esm.dao.request.OrderSearchCriteria;
import com.epam.esm.dao.sort.SortBy;
import com.epam.esm.dao.sort.SortType;
import com.epam.esm.service.exception.ServiceException;
import com.epam.esm.web.controller.OrderController;
import com.epam.esm.web.dto.GiftCertificateDto;
import com.epam.esm.web.dto.OrderDto;
import com.epam.esm.web.dto.TagDto;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class OrderLinkBuilder implements ModelLinkBuilder<OrderDto> {

    private static ModelLinkBuilder<GiftCertificateDto> certificateLinkBuilder;
    private static ModelLinkBuilder<TagDto> tagLinkBuilder;

    private static final String ALL_ORDERS = "orders";
    private static final String CURRENT_ORDER = "self";

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final OrderSearchCriteria defaultRequestBody = OrderSearchCriteria.getDefaultUserRequestBody();

    @PostConstruct
    public void init() {
        OrderLinkBuilder.certificateLinkBuilder = new CertificateLinkBuilder();
        OrderLinkBuilder.tagLinkBuilder = new TagLinkBuilder();
    }

    @Override
    public void linkToModel(EntityModel<OrderDto> modelDto) throws ServiceException {
        modelDto.add(linkTo(methodOn(OrderController.class).getOrder(
                Objects.requireNonNull(modelDto.getContent()).getId())).withRel(CURRENT_ORDER));

        for (EntityModel<GiftCertificateDto> certificate: modelDto.getContent().getGiftCertificateList()) {
            for (EntityModel<TagDto> tag: Objects.requireNonNull(certificate.getContent()).getTags()) {
                tagLinkBuilder.linkToModel(tag);
            }
            certificateLinkBuilder.linkToModel(certificate);
        }
    }

    @Override
    public void linkToModelPage(CollectionModel<EntityModel<OrderDto>> collectionModel, int page, int size,
                                SortType sortType, SortBy sortBy) throws ServiceException {
        collectionModel.add(getLinkToOrdersPage(page, size, ALL_ORDERS, sortType, sortBy));
    }

    @Override
    public void linkToFirstModelPage(EntityModel<OrderDto> tagDto, SortType sortType, SortBy sortBy)
            throws ServiceException {
        tagDto.add(getLinkToOrdersPage(DEFAULT_PAGE, DEFAULT_SIZE, ALL_ORDERS, sortType, sortBy));
    }

    @Override
    public void linkToNextModelPage(CollectionModel<EntityModel<OrderDto>> collectionModel, int page, int size,
                                    SortType sortType, SortBy sortBy) throws ServiceException {
        collectionModel.add(getLinkToOrdersPage(page + 1, size, "next", sortType, sortBy));
    }

    @Override
    public void linkToPrevModelPage(CollectionModel<EntityModel<OrderDto>> collectionModel, int page, int size,
                                    SortType sortType, SortBy sortBy) throws ServiceException {
        collectionModel.add(getLinkToOrdersPage(page - 1, size, "prev", sortType, sortBy));
    }

    @Override
    public void linkToLastModelPage(CollectionModel<EntityModel<OrderDto>> collectionModel, int lastPage, int size, SortType sortType, SortBy sortBy) throws ServiceException {
        collectionModel.add(getLinkToOrdersPage(lastPage, size, "last", sortType, sortBy));
    }

    private Link getLinkToOrdersPage(int page, int size, String rel, SortType sortType, SortBy sortBy)
            throws ServiceException {
        return linkTo(methodOn(OrderController.class).getOrders(defaultRequestBody, page, size,
                sortType, sortBy)).withRel(rel);
    }
}
