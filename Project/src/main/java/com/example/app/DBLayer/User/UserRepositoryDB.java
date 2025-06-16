package com.example.app.DBLayer.User;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.IUserRepository;
import com.example.app.DomainLayer.Member;

@Profile("!no-db & !test")
public interface UserRepositoryDB extends JpaRepository<Member, Integer>, IUserRepository {


}
