package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A saved team (genshinEquipes.json). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String usage = "Libre";
    public List<TeamMember> members = new ArrayList<>();

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (name == null) name = "";
        if (usage == null) usage = "Libre";
        if (members == null) members = new ArrayList<>();
    }

    public Team copy() {
        Team t = new Team();
        t.id = id;
        t.name = name;
        t.usage = usage;
        for (TeamMember m : members) t.members.add(m.copy());
        return t;
    }

    @Override
    public String toString() {
        return name;
    }
}
