package com.familytree.service;

import com.familytree.dto.search.SearchRequest;
import com.familytree.dto.search.SearchResult;
import com.familytree.model.Gender;
import com.familytree.model.Individual;
import com.familytree.repository.IndividualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for searching individuals across trees
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final IndividualRepository individualRepository;
    private final PermissionService permissionService;
    private final EntityManager entityManager;

    /**
     * Search individuals in a specific tree
     */
    @Transactional(readOnly = true)
    public Page<SearchResult> searchInTree(UUID treeId, UUID userId, SearchRequest request) {
        // Verify user has access to tree
        if (!permissionService.canViewTree(userId, treeId)) {
            throw new RuntimeException("You don't have permission to view this tree");
        }

        log.info("Searching tree {} with query: {}", treeId, request.getQuery());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Individual> query = cb.createQuery(Individual.class);
        Root<Individual> root = query.from(Individual.class);

        List<Predicate> predicates = buildPredicates(cb, root, treeId, request);

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(
                cb.desc(cb.concat(
                        cb.coalesce(root.get("givenName"), ""),
                        cb.coalesce(root.get("surname"), "")
                ))
        );

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        TypedQuery<Individual> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Individual> individuals = typedQuery.getResultList();

        // Convert to search results
        List<SearchResult> results = individuals.stream()
                .map(SearchResult::fromEntity)
                .collect(Collectors.toList());

        // Calculate relevance scores if query provided
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            calculateRelevanceScores(results, request.getQuery());
        }

        return new PageImpl<>(results, pageable, countResults(treeId, request));
    }

    /**
     * Search individuals across all accessible trees
     */
    @Transactional(readOnly = true)
    public List<SearchResult> globalSearch(UUID userId, String query, int maxResults) {
        log.info("Global search by user {} with query: {}", userId, query);

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchPattern = "%" + query.toLowerCase() + "%";

        // Search across all trees user has access to
        List<Individual> individuals = individualRepository.findAll().stream()
                .filter(ind -> permissionService.canViewTree(userId, ind.getTree().getId()))
                .filter(ind -> matchesQuery(ind, searchPattern))
                .limit(maxResults)
                .collect(Collectors.toList());

        List<SearchResult> results = individuals.stream()
                .map(SearchResult::fromEntity)
                .collect(Collectors.toList());

        calculateRelevanceScores(results, query);

        // Sort by relevance score
        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        return results;
    }

    /**
     * Build search predicates from request
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Individual> root, UUID treeId, SearchRequest request) {
        List<Predicate> predicates = new ArrayList<>();

        // Tree filter
        predicates.add(cb.equal(root.get("tree").get("id"), treeId));

        // Text search (name, places)
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            String searchPattern = "%" + request.getQuery().toLowerCase() + "%";

            Predicate givenNameMatch = cb.like(cb.lower(root.get("givenName")), searchPattern);
            Predicate surnameMatch = cb.like(cb.lower(root.get("surname")), searchPattern);
            Predicate birthPlaceMatch = cb.like(cb.lower(root.get("birthPlace")), searchPattern);
            Predicate deathPlaceMatch = cb.like(cb.lower(root.get("deathPlace")), searchPattern);

            predicates.add(cb.or(givenNameMatch, surnameMatch, birthPlaceMatch, deathPlaceMatch));
        }

        // Gender filter
        if (request.getGender() != null && !request.getGender().isEmpty()) {
            try {
                Gender gender = Gender.valueOf(request.getGender().toUpperCase());
                predicates.add(cb.equal(root.get("gender"), gender));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender filter: {}", request.getGender());
            }
        }

        // Birth year range
        if (request.getBirthYearFrom() != null) {
            LocalDate fromDate = LocalDate.of(request.getBirthYearFrom(), 1, 1);
            predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"), fromDate));
        }
        if (request.getBirthYearTo() != null) {
            LocalDate toDate = LocalDate.of(request.getBirthYearTo(), 12, 31);
            predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), toDate));
        }

        // Death year range
        if (request.getDeathYearFrom() != null) {
            LocalDate fromDate = LocalDate.of(request.getDeathYearFrom(), 1, 1);
            predicates.add(cb.greaterThanOrEqualTo(root.get("deathDate"), fromDate));
        }
        if (request.getDeathYearTo() != null) {
            LocalDate toDate = LocalDate.of(request.getDeathYearTo(), 12, 31);
            predicates.add(cb.lessThanOrEqualTo(root.get("deathDate"), toDate));
        }

        // Birth place filter
        if (request.getBirthPlace() != null && !request.getBirthPlace().isEmpty()) {
            String placePattern = "%" + request.getBirthPlace().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("birthPlace")), placePattern));
        }

        // Death place filter
        if (request.getDeathPlace() != null && !request.getDeathPlace().isEmpty()) {
            String placePattern = "%" + request.getDeathPlace().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("deathPlace")), placePattern));
        }

        return predicates;
    }

    /**
     * Count search results
     */
    private long countResults(UUID treeId, SearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Individual> root = query.from(Individual.class);

        List<Predicate> predicates = buildPredicates(cb, root, treeId, request);

        query.select(cb.count(root));
        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Check if individual matches query
     */
    private boolean matchesQuery(Individual ind, String searchPattern) {
        String lowerPattern = searchPattern.toLowerCase().replace("%", "");

        if (ind.getGivenName() != null && ind.getGivenName().toLowerCase().contains(lowerPattern)) {
            return true;
        }
        if (ind.getSurname() != null && ind.getSurname().toLowerCase().contains(lowerPattern)) {
            return true;
        }
        if (ind.getBirthPlace() != null && ind.getBirthPlace().toLowerCase().contains(lowerPattern)) {
            return true;
        }
        if (ind.getDeathPlace() != null && ind.getDeathPlace().toLowerCase().contains(lowerPattern)) {
            return true;
        }

        return false;
    }

    /**
     * Calculate relevance scores based on query match
     */
    private void calculateRelevanceScores(List<SearchResult> results, String query) {
        String lowerQuery = query.toLowerCase();

        for (SearchResult result : results) {
            double score = 0.0;

            // Exact name match gets highest score
            String fullName = (result.getGivenName() + " " + result.getSurname()).toLowerCase();
            if (fullName.equals(lowerQuery)) {
                score += 10.0;
            } else if (fullName.startsWith(lowerQuery)) {
                score += 5.0;
            } else if (fullName.contains(lowerQuery)) {
                score += 2.0;
            }

            // Given name match
            if (result.getGivenName() != null) {
                String givenName = result.getGivenName().toLowerCase();
                if (givenName.equals(lowerQuery)) {
                    score += 8.0;
                } else if (givenName.startsWith(lowerQuery)) {
                    score += 4.0;
                } else if (givenName.contains(lowerQuery)) {
                    score += 1.5;
                }
            }

            // Surname match
            if (result.getSurname() != null) {
                String surname = result.getSurname().toLowerCase();
                if (surname.equals(lowerQuery)) {
                    score += 8.0;
                } else if (surname.startsWith(lowerQuery)) {
                    score += 4.0;
                } else if (surname.contains(lowerQuery)) {
                    score += 1.5;
                }
            }

            // Place matches (lower weight)
            if (result.getBirthPlace() != null && result.getBirthPlace().toLowerCase().contains(lowerQuery)) {
                score += 0.5;
            }
            if (result.getDeathPlace() != null && result.getDeathPlace().toLowerCase().contains(lowerQuery)) {
                score += 0.5;
            }

            result.setRelevanceScore(score);
        }
    }

    /**
     * Helper class for paginated results
     */
    private static class PageImpl<T> implements Page<T> {
        private final List<T> content;
        private final Pageable pageable;
        private final long total;

        public PageImpl(List<T> content, Pageable pageable, long total) {
            this.content = content;
            this.pageable = pageable;
            this.total = total;
        }

        @Override
        public int getTotalPages() {
            return (int) Math.ceil((double) total / pageable.getPageSize());
        }

        @Override
        public long getTotalElements() {
            return total;
        }

        @Override
        public int getNumber() {
            return pageable.getPageNumber();
        }

        @Override
        public int getSize() {
            return pageable.getPageSize();
        }

        @Override
        public int getNumberOfElements() {
            return content.size();
        }

        @Override
        public List<T> getContent() {
            return content;
        }

        @Override
        public boolean hasContent() {
            return !content.isEmpty();
        }

        @Override
        public org.springframework.data.domain.Sort getSort() {
            return pageable.getSort();
        }

        @Override
        public boolean isFirst() {
            return !hasPrevious();
        }

        @Override
        public boolean isLast() {
            return !hasNext();
        }

        @Override
        public boolean hasNext() {
            return getNumber() + 1 < getTotalPages();
        }

        @Override
        public boolean hasPrevious() {
            return getNumber() > 0;
        }

        @Override
        public Pageable nextPageable() {
            return hasNext() ? pageable.next() : Pageable.unpaged();
        }

        @Override
        public Pageable previousPageable() {
            return hasPrevious() ? pageable.previousOrFirst() : Pageable.unpaged();
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return content.iterator();
        }

        @Override
        public <U> Page<U> map(java.util.function.Function<? super T, ? extends U> converter) {
            return new PageImpl<>(content.stream().map(converter).collect(Collectors.toList()), pageable, total);
        }
    }
}
