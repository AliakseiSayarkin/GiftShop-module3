package com.epam.esm.web.controller;

import com.epam.esm.dao.sort.SortBy;
import com.epam.esm.dao.sort.SortType;
import com.epam.esm.model.Tag;
import com.epam.esm.service.TagService;
import com.epam.esm.service.exception.ServiceException;
import com.epam.esm.dao.request.TagSearchCriteria;
import com.epam.esm.web.dto.TagDto;
import com.epam.esm.web.hateoas.ModelAssembler;
import com.epam.esm.web.hateoas.RepresentationModel;
import com.epam.esm.web.hateoas.TagLinkBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;


@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;
    private final ModelAssembler<TagDto> modelAssembler;

    @Autowired
    public TagController(TagService tagService, ModelAssembler<TagDto> modelAssembler) {
        this.tagService = tagService;
        this.modelAssembler = modelAssembler;
    }

    @PostConstruct
    public void init() {
        modelAssembler.setModelLinkBuilder(new TagLinkBuilder());
    }

    @GetMapping
    public CollectionModel<EntityModel<TagDto>> getTags(
            @RequestBody(required = false) TagSearchCriteria requestBody,
            @RequestParam int page, @RequestParam int size,
            @RequestParam SortType sortType, @RequestParam SortBy sortBy) throws ServiceException {
        int lastPage = tagService.getLastPage(size);
        PagedModel.PageMetadata pageMetadata = new PagedModel.PageMetadata(
                size, page, (long) lastPage * size, lastPage);
        RepresentationModel model = new RepresentationModel(pageMetadata, sortType, sortBy);
        modelAssembler.setRepresentationModel(model);

        return modelAssembler.toCollectionModel(
                TagDto.of(tagService.getAllTagsByPage(requestBody, page, size, sortType, sortBy)));
    }

    @GetMapping("/{id}")
    public EntityModel<TagDto> getTag(@PathVariable int id) throws ServiceException {
        return modelAssembler.toModel(TagDto.of(tagService.getTagById(id)));
    }

    @PostMapping
    public EntityModel<TagDto> addTag(@RequestBody Tag tag) throws ServiceException {
        return modelAssembler.toModel(TagDto.of(tagService.addTag(tag)));
    }

    @DeleteMapping("/{id}")
    public HttpStatus deleteTag(@PathVariable int id) throws ServiceException {
        tagService.deleteTag(id);
        return HttpStatus.OK;
    }
}
