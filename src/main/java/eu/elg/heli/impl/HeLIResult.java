package eu.elg.heli.impl;

public class HeLIResult {

    public final String language;
    public final float score;

    public HeLIResult(String language, float score) {
        this.language = language;
        this.score = score;
    }

    @Override
    public String toString() {
        return "(" + this.language + ", " + this.score + ")";
    }
}
