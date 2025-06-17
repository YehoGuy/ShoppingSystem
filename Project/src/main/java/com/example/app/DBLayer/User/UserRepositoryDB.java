package com.example.app.DBLayer.User;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Member;

public interface UserRepositoryDB extends JpaRepository<Member, Integer> {

}
