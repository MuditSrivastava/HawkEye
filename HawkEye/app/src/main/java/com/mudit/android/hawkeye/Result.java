package com.mudit.android.hawkeye;

/**
 * Created by DELL on 3/12/2017.
 */

public class Result {

    String name;
    Double score;

    public Result(){}

    public Result(String name, Double score){
        this.name=name;
        this.score=score;
    }

    public String getname(){
        return name;
    }

    public Double getscore(){
        return score;
    }

    public void setname(String name){

        this.name=name;
    }

    public void setscore(Double score){
        this.score=score;
    }
}
