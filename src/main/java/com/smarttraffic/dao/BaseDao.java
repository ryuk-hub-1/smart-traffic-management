package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Base Data Access Object interface.
 * Demonstrates Abstraction, Interfaces, and Generics in Advanced Java.
 *
 * @param <T> Domain entity type
 * @param <ID> Primary key identifier type
 */
public interface BaseDao<T, ID> {

    Optional<T> findById(ID id) throws DatabaseException;

    List<T> findAll() throws DatabaseException;

    T save(T entity) throws DatabaseException;

    boolean update(T entity) throws DatabaseException;

    boolean deleteById(ID id) throws DatabaseException;
}
