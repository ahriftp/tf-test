package com.tecforte.blog.web.rest;

import com.tecforte.blog.service.BlogService;
import com.tecforte.blog.web.rest.errors.BadRequestAlertException;
import com.tecforte.blog.service.dto.BlogDTO;
import com.tecforte.blog.service.EntryService;
import com.tecforte.blog.service.dto.EntryDTO;
import com.tecforte.blog.service.dto.KeywordsDTO;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST controller for managing {@link com.tecforte.blog.domain.Blog}.
 */
@RestController
@RequestMapping("/api")
public class BlogResource {

    private final Logger log = LoggerFactory.getLogger(BlogResource.class);

    private static final String ENTITY_NAME = "blog";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BlogService blogService;

    public BlogResource(BlogService blogService) {
        this.blogService = blogService;
    }

    @Autowired
    EntryService entryService;

    /**
     * {@code POST  /blogs} : Create a new blog.
     *
     * @param blogDTO the blogDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new blogDTO, or with status {@code 400 (Bad Request)} if the
     *         blog has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/blogs")
    public ResponseEntity<BlogDTO> createBlog(@Valid @RequestBody BlogDTO blogDTO) throws URISyntaxException {
        log.debug("REST request to save Blog : {}", blogDTO);
        if (blogDTO.getId() != null) {
            throw new BadRequestAlertException("A new blog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        blogDTO.setEntryCount(0);
        BlogDTO result = blogService.save(blogDTO);
        return ResponseEntity
                .created(new URI("/api/blogs/" + result.getId())).headers(HeaderUtil
                        .createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * {@code PUT  /blogs} : Updates an existing blog.
     *
     * @param blogDTO the blogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated blogDTO, or with status {@code 400 (Bad Request)} if the
     *         blogDTO is not valid, or with status
     *         {@code 500 (Internal Server Error)} if the blogDTO couldn't be
     *         updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/blogs")
    public ResponseEntity<BlogDTO> updateBlog(@Valid @RequestBody BlogDTO blogDTO) throws URISyntaxException {
        log.debug("REST request to update Blog : {}", blogDTO);
        if (blogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        BlogDTO result = blogService.save(blogDTO);
        return ResponseEntity.ok().headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, blogDTO.getId().toString()))
                .body(result);
    }

    /**
     * {@code GET  /blogs} : get all the blogs.
     *
     * 
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     *         of blogs in body.
     */
    @GetMapping("/blogs")
    public List<BlogDTO> getAllBlogs() {
        log.debug("REST request to get all Blogs");
        return blogService.findAll();
    }

    /**
     * {@code GET  /blogs/:id} : get the "id" blog.
     *
     * @param id the id of the blogDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the blogDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/blogs/{id}")
    public ResponseEntity<BlogDTO> getBlog(@PathVariable Long id) {
        log.debug("REST request to get Blog : {}", id);
        Optional<BlogDTO> blogDTO = blogService.findOne(id);
        return ResponseUtil.wrapOrNotFound(blogDTO);
    }

    /**
     * {@code DELETE  /blogs/:id} : delete the "id" blog.
     *
     * @param id the id of the blogDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        log.debug("REST request to delete Blog : {}", id);
        blogService.delete(id);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                .build();
    }

    /**
     * {@code CLEAN  /blogs/clean} : deletes all blog entries consisting keywords.
     *
     * @param keywords consists the keywords to check for
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @DeleteMapping("/blogs/clean")
    public ResponseEntity cleanBlog(@Valid @RequestBody KeywordsDTO keywordsDTO) {

        Pageable pageable = PageRequest.of(0, 100);
        while (true) {
            Page<EntryDTO> entryEntities = entryService.findAll(pageable);

            List<EntryDTO> entryEntitiesList = entryEntities.getContent();
            entryEntitiesList.forEach(entry -> {
                checkEntryContent(entry, keywordsDTO);
            });

            if (!entryEntities.hasNext()) {
                break;
            }
            pageable = entryEntities.nextPageable();
        }

        return new ResponseEntity<>("Purged entries with keywords matching", HttpStatus.OK);
    }

    @DeleteMapping("blogs/{id}/clean")
    public ResponseEntity cleanSpecificBlog(@Valid @RequestBody KeywordsDTO keywordsDTO, @PathVariable Long id) {
        Pageable pageable = PageRequest.of(0, 100);
        while (true) {
            Page<EntryDTO> entryEntities = entryService.findAll(pageable);

            List<EntryDTO> entryEntitiesList = entryEntities.getContent();
            entryEntitiesList.forEach(entry -> {
                Long entryBlogId = entry.getBlogId();
                if (entryBlogId.equals(id)) {
                    checkEntryContent(entry, keywordsDTO);
                }
            });

            if (!entryEntities.hasNext()) {
                break;
            }
            pageable = entryEntities.nextPageable();
        }
        return new ResponseEntity<>("Purged entries with keywords matching from blog id: " + id, HttpStatus.OK);

    }

    public static boolean stringMatchRegex(String x, String y) {
        Pattern pattern = Pattern.compile(y, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(x);
        return matcher.find();
    }

    public void checkEntryContent(EntryDTO entry, KeywordsDTO keywordsDTO) {
        for (String y : keywordsDTO.getKeywords()) {
            if (stringMatchRegex(entry.getTitle(), y) || stringMatchRegex(entry.getContent(), y)) {
                log.debug("Deleted entry: " + entry.getId());
                entryService.delete(entry.getId());
            }
        }
    }
}
