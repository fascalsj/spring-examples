package hello.dao.impl;

import hello.container.FieldHolder;
import hello.dao.AbstractDao;
import hello.util.ReflectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.springframework.util.ObjectUtils.isEmpty;

public abstract class AbstractDaoImpl<T, ID extends Serializable> implements AbstractDao<T, ID> {

    protected abstract Class<T> getPersistentClass();

    @PersistenceContext
    private EntityManager em;

    protected Session getSession() {
        return em.unwrap(Session.class);
    }

    protected Criteria createEntityCriteria() {
        return getSession()
                .createCriteria(getPersistentClass())
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
    }

    @Override
    public List<T> getAll() {
        return createEntityCriteria().list();
    }

    @Override
    public Optional<T> getById(ID id) {
        T entity = getSession().get(getPersistentClass(), id);
        return Optional.ofNullable(entity);
    }

    @Override
    public void persist(T entity) {
        getSession().persist(entity);
    }

    @Override
    public void saveOrUpdate(T entity) {
        getSession().saveOrUpdate(entity);
    }

    @Override
    public void delete(T entity) {
        getSession().delete(entity);
    }

    @Override
    public List<T> getByProps(Map<String, List<?>> props) {
        Criteria criteria = createEntityCriteria();
        props.forEach((fieldName, values) -> createCriteriaByFieldNameAndValues(criteria, fieldName, values));

        return criteria.list();
    }

    private void createCriteriaByFieldNameAndValues(Criteria criteria, String fieldName, List<?> values) {
        Class<?> fieldType = getFieldType(fieldName);

        if (values.isEmpty()) return;

        if (fieldType.isAssignableFrom(Double.class))
            criteria.add(Restrictions.in(fieldName, values.stream().map(x -> ((Number)x).doubleValue()).collect(Collectors.toSet())));

        else if (fieldType.isAssignableFrom(Long.class))
            criteria.add(Restrictions.in(fieldName, values.stream().map(x -> ((Number)x).longValue()).collect(Collectors.toSet())));

        else if (fieldType.isAssignableFrom(Float.class))
            criteria.add(Restrictions.in(fieldName, values.stream().map(x -> ((Number)x).floatValue()).collect(Collectors.toSet())));

        else if (fieldType.isAssignableFrom(Integer.class))
            criteria.add(Restrictions.in(fieldName, values.stream().map(x -> ((Number)x).intValue()).collect(Collectors.toSet())));

        else if (fieldType.isAssignableFrom(Short.class))
            criteria.add(Restrictions.in(fieldName, values.stream().map(x -> ((Number)x).shortValue()).collect(Collectors.toSet())));

        else
            criteria.add(Restrictions.in(fieldName, values));
    }

    private Class<?> getFieldType(String fieldName) {
        Field field = ReflectionUtils.getField(getPersistentClass(), fieldName)
                .orElseThrow(() -> new RuntimeException(String.format("Cannot find fieldName: '%s'", fieldName)));
        return field.getType();
    }

    @Override
    public List<T> getByFields(Collection<FieldHolder> fieldHolders) {
        if (isEmpty(fieldHolders)) {
            return emptyList();
        }

        Criteria criteria = createEntityCriteria();

        for (FieldHolder fieldHolder : fieldHolders) {
            Assert.notNull(fieldHolder.getFieldName(), "Field name cannot be null");

            if (!fieldHolder.getIsRelationId()) {
                criteria = getCriteriaEqByFieldWithCast(criteria, fieldHolder.getFieldName(), fieldHolder.getValue());
                continue;
            }

            criteria = getCriteriaAliasRelationIdWithCast(criteria, fieldHolder);
        }
        return criteria.list();
    }

    /**
     * Important! equals without cast type - do the cast yourself
     *
     * @param criteria criteria
     * @param fieldName fieldName
     * @param fieldValue fieldValue
     * @return Criteria
     */
    protected Criteria getCriteriaEqByField(Criteria criteria, String fieldName, Object fieldValue) {
        criteria.add(Restrictions.eq(fieldName, fieldValue));
        return criteria;
    }

    protected Criteria getCriteriaEqByFieldWithCast(Criteria criteria, String fieldName, Object fieldValue) {
        Object fieldValueCasted = fieldValue == null ? null : ReflectionUtils.castFieldValue(getPersistentClass(), fieldName, fieldValue);

        criteria.add(Restrictions.eq(fieldName, fieldValueCasted));
        return criteria;
    }

    private Criteria getCriteriaAliasRelationIdWithCast(Criteria criteria, FieldHolder fieldHolder) {
        String fieldName = fieldHolder.getFieldName();

        Field field = ReflectionUtils.getField(getPersistentClass(), fieldName)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find field name: '" + fieldName + "'"));

        Class<?> relationObjectClass = field.getType();
        Object castedId = ReflectionUtils.castFieldValue(relationObjectClass, "id", fieldHolder.getValue());

        Criteria criteriaWithAlias = criteria.createAlias(fieldName, fieldName);
        return getCriteriaEqByField(criteriaWithAlias, fieldName + ".id", castedId);
    }
}
