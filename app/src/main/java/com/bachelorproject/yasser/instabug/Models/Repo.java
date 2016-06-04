package com.bachelorproject.yasser.instabug.Models;


import java.io.Serializable;

public class Repo implements Serializable {
    String name;
    String description;
    String login;
    String fork;
    String owner_url;
    String repo_url;

    public String getFork() {
        return fork;
    }

    public String getRepo_url() {
        return repo_url;
    }

    public String getOwner_url() {

        return owner_url;
    }

    public Repo(String name , String description, String login, String fork, String owner_url, String repo_url){
        this.name=name;
        this.description=description;
        this.login=login;
        this.fork=fork;
        this.owner_url=owner_url;
        this.repo_url=repo_url;


    }

    public String getLogin() {
        return login;
    }

    public String getDescription() {

        return description;
    }

    public String getName() {

        return name;
    }
}