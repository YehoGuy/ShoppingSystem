package main.java.DomainLayer;

public interface IUserRepository {

    User getUserById(int id); // Retrieve a user by their ID

    User getUser(User user); // Retrieve a user by their object

    void addUser(User user); // Add a new user to the repository

    void removeUserById(int id); // Remove a user by their ID

    void removeUserByUserObject(User user); // Remove a user by their object
}