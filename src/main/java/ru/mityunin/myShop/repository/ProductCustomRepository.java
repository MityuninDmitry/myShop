package ru.mityunin.myShop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.model.Product;

@Repository
public class ProductCustomRepository {
    private final R2dbcEntityTemplate template;

    public ProductCustomRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    public Flux<Product> findAll(Pageable pageable) {
        return template.select(Product.class)
                .matching(Query.empty().sort(pageable.getSort()).with(pageable))
                .all();
    }

    public Flux<Product> findAllFiltered(String filter, Pageable pageable) {
        return template.select(Product.class)
                .matching(Query.query(
                        Criteria.where("name").like("%" + filter + "%").ignoreCase(true)
                                .or("description").like("%" + filter + "%").ignoreCase(true)
                ).sort(pageable.getSort()).with(pageable))
                .all();
    }
}
