package com.example.app.DBLayer.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.IUserRepository;
import com.example.app.DomainLayer.Member;

@Repository
public interface UserRepositoryDB extends JpaRepository<Member, Integer>, IUserRepository {


}
