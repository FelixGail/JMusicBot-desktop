/*
 * JMusicBot
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 0.4.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.github.bjoernpetersen.deskbot.api.swag.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * PlayerState
 */

public class PlayerState {

  /**
   * Gets or Sets state
   */
  public enum StateEnum {
    PLAY("PLAY"),

    PAUSE("PAUSE"),

    STOP("STOP"),

    ERROR("ERROR");

    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StateEnum fromValue(String text) {
      for (StateEnum b : StateEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("state")
  private StateEnum state = null;

  @JsonProperty("songEntry")
  private SongEntry songEntry = null;

  public PlayerState state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * Get state
   *
   * @return state
   **/
  @JsonProperty("state")
  @ApiModelProperty(required = true, value = "")
  @NotNull
  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  public PlayerState songEntry(SongEntry songEntry) {
    this.songEntry = songEntry;
    return this;
  }

  /**
   * Get songEntry
   *
   * @return songEntry
   **/
  @JsonProperty("songEntry")
  @ApiModelProperty(value = "")
  public SongEntry getSongEntry() {
    return songEntry;
  }

  public void setSongEntry(SongEntry songEntry) {
    this.songEntry = songEntry;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlayerState playerState = (PlayerState) o;
    return Objects.equals(this.state, playerState.state) &&
        Objects.equals(this.songEntry, playerState.songEntry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, songEntry);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PlayerState {\n");

    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    songEntry: ").append(toIndentedString(songEntry)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

