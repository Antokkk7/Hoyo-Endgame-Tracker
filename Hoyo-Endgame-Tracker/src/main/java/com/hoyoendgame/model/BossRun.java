package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** One boss fight of a Stygian Onslaught clear. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BossRun {
    public String name = "";
    /** Fight duration in seconds. */
    public int seconds;
    public List<TeamMember> team = new ArrayList<>();

    public void normalize() {
        if (name == null) name = "";
        if (team == null) team = new ArrayList<>();
    }
}
