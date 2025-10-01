package com.mg.nmlonline.model.player;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;

@Entity
@Table(name = "CREDENTIALS")
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String password;
    private int money;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
}
