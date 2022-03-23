package eu.elg.heli.impl;

public class HeLIResult {

  public final String language3;

  public final String language2;

  public final float score;

  public HeLIResult(String language3, String language2, float score) {
    this.language3 = language3;
    this.language2 = language2;
    this.score = score;
  }
}
