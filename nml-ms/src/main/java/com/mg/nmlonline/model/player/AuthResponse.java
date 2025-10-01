package com.mg.nmlonline.model.player;

public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private int money;

    public AuthResponse(String token, Long id, String name, int money) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.money = money;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
}
