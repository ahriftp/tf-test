package com.tecforte.blog.web.rest;

import com.tecforte.blog.service.EntryService;
import com.tecforte.blog.web.rest.errors.BadRequestAlertException;
import com.tecforte.blog.service.dto.EntryDTO;
import com.tecforte.blog.domain.enumeration.Emoji;
import com.tecforte.blog.service.BlogService;
import com.tecforte.blog.service.dto.BlogDTO;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST controller for managing {@link com.tecforte.blog.domain.Entry}.
 */
@RestController
@RequestMapping("/api")
public class EntryResource {

    private final Logger log = LoggerFactory.getLogger(EntryResource.class);

    private static final String ENTITY_NAME = "entry";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EntryService entryService;

    public EntryResource(EntryService entryService) {
        this.entryService = entryService;
    }

    @Autowired
    BlogService blogService;

    static Emoji[] positiveEmoji = { Emoji.LIKE, Emoji.HAHA };
    static Emoji[] negativeEmoji = { Emoji.ANGRY, Emoji.SAD };
    static String[] positiveWords = { "love", "happy", "trust" };
    static String[] negativeWords = { "sad", "fear", "lonely" };

    /**
     * {@code POST  /entries} : Create a new entry.
     *
     * @param entryDTO the entryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new entryDTO, or with status {@code 400 (Bad Request)} if
     *         the entry has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/entries")
    public ResponseEntity<EntryDTO> createEntry(@Valid @RequestBody EntryDTO entryDTO) throws URISyntaxException {
        log.debug("REST request to save Entry : {}", entryDTO);
        if (entryDTO.getId() != null) {
            throw new BadRequestAlertException("A new entry cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Optional<BlogDTO> blogDTO = blogService.findOne(entryDTO.getBlogId());

        if (blogDTO.isPresent()) {
            boolean blogEmotion = blogDTO.get().isPositive();
            String entryTitle = entryDTO.getTitle();
            String entryContent = entryDTO.getContent().toLowerCase();
            Emoji entryEmoji = entryDTO.getEmoji();

            checkEntryContents(blogEmotion, entryEmoji, entryTitle, entryContent);
        } else {
            throw new BadRequestAlertException("Blog does not exist", ENTITY_NAME, "blognotfound");
        }

        EntryDTO result = entryService.save(entryDTO);
        return ResponseEntity
                .created(new URI("/api/entries/" + result.getId())).headers(HeaderUtil
                        .createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * {@code PUT  /entries} : Updates an existing entry.
     *
     * @param entryDTO the entryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated entryDTO, or with status {@code 400 (Bad Request)} if the
     *         entryDTO is not valid, or with status
     *         {@code 500 (Internal Server Error)} if the entryDTO couldn't be
     *         updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/entries")
    public ResponseEntity<EntryDTO> updateEntry(@Valid @RequestBody EntryDTO entryDTO) throws URISyntaxException {
        log.debug("REST request to update Entry : {}", entryDTO);
        if (entryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        Optional<BlogDTO> blogDTO = blogService.findOne(entryDTO.getBlogId());

        if (blogDTO.isPresent()) {
            boolean blogEmotion = blogDTO.get().isPositive();
            String entryTitle = entryDTO.getTitle();
            String entryContent = entryDTO.getContent().toLowerCase();
            Emoji entryEmoji = entryDTO.getEmoji();

            checkEntryContents(blogEmotion, entryEmoji, entryTitle, entryContent);

        } else {
            throw new BadRequestAlertException("Blog does not exist", ENTITY_NAME, "blognotfound");
        }

        EntryDTO result = entryService.save(entryDTO);
        return ResponseEntity.ok().headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, entryDTO.getId().toString()))
                .body(result);
    }

    /**
     * {@code GET  /entries} : get all the entries.
     *
     * 
     * @param pageable the pagination information.
     * 
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     *         of entries in body.
     */
    @GetMapping("/entries")
    public ResponseEntity<List<EntryDTO>> getAllEntries(Pageable pageable) {
        log.debug("REST request to get a page of Entries");
        Page<EntryDTO> page = entryService.findAll(pageable);
        HttpHeaders headers = PaginationUtil
                .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /entries/:id} : get the "id" entry.
     *
     * @param id the id of the entryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the entryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/entries/{id}")
    public ResponseEntity<EntryDTO> getEntry(@PathVariable Long id) {
        log.debug("REST request to get Entry : {}", id);
        Optional<EntryDTO> entryDTO = entryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(entryDTO);
    }

    /**
     * {@code DELETE  /entries/:id} : delete the "id" entry.
     *
     * @param id the id of the entryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        log.debug("REST request to delete Entry : {}", id);
        entryService.delete(id);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                .build();
    }

    public static void checkContent(boolean blogEmotion, Emoji entryEmoji, String entryTitle, String entryContent) {
        if (blogEmotion) {
            for (Emoji x : negativeEmoji) {
                if (x == entryEmoji) {
                    throw new BadRequestAlertException("Invalid Emoji", ENTITY_NAME, "invalidEmoji");
                }
            }

            for (String y : negativeWords) {
                y = ".*\\b" + y + "\\b.*";
                if (stringMatchRegex(entryTitle, y) || stringMatchRegex(entryContent, y)) {
                    throw new BadRequestAlertException("Invalid Content", ENTITY_NAME, "invalidContent");
                }
            }

        } else {
            for (Emoji x : positiveEmoji) {
                if (x == entryEmoji) {
                    throw new BadRequestAlertException("Invalid Emoji", ENTITY_NAME, "invalidEmoji");
                }
            }

            for (String y : positiveWords) {
                y = ".*\\b" + y + "\\b.*";
                if (stringMatchRegex(entryTitle, y) || stringMatchRegex(entryContent, y)) {
                    throw new BadRequestAlertException("Invalid Content", ENTITY_NAME, "invalidContent");
                }
            }
        }
    }

    // Uses regex to check for substring (exact word)
    public static boolean stringMatchRegex(String x, String y) {
        Pattern pattern = Pattern.compile(y, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(x);
        return matcher.find();
    }

    // Runs rules to check if contents are valid for the entry
    public static void checkEntryContents(boolean blogEmotion, Emoji entryEmoji, String entryTitle,
            String entryContent) {
        if (blogEmotion) {
            for (Emoji x : negativeEmoji) {
                if (x == entryEmoji) {
                    throw new BadRequestAlertException("Invalid Emoji", ENTITY_NAME, "invalidEmoji");
                }
            }

            for (String y : negativeWords) {
                y = ".*\\b" + y + "\\b.*";
                if (stringMatchRegex(entryTitle, y) || stringMatchRegex(entryContent, y)) {
                    throw new BadRequestAlertException("Invalid Content", ENTITY_NAME, "invalidContent");
                }
            }

        } else {
            for (Emoji x : positiveEmoji) {
                if (x == entryEmoji) {
                    throw new BadRequestAlertException("Invalid Emoji", ENTITY_NAME, "invalidEmoji");
                }
            }

            for (String y : positiveWords) {
                y = ".*\\b" + y + "\\b.*";
                if (stringMatchRegex(entryTitle, y) || stringMatchRegex(entryContent, y)) {
                    throw new BadRequestAlertException("Invalid Content", ENTITY_NAME, "invalidContent");
                }
            }
        }
    }
}
