package com.example.lock.repository;

import com.example.lock.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("""
        select u from User u
        where (:search is null
               or lower(u.fullName) like lower(concat('%', cast(:search as string), '%'))
               or lower(u.email) like lower(concat('%', cast(:search as string), '%')))
        order by u.fullName
        """)
    List<User> search(@Param("search") String search);
}
