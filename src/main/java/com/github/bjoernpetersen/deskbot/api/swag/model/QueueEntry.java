/*
 * JMusicBot
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 0.6.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.github.bjoernpetersen.deskbot.api.swag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * QueueEntry
 */

public class QueueEntry {

  @JsonProperty("song")
  private Song song = null;

  @JsonProperty("userName")
  private String userName = null;

  public QueueEntry song(Song song) {
    this.song = song;
    return this;
  }

  /**
   * Get song
   *
   * @return song
   **/
  @JsonProperty("song")
  @ApiModelProperty(required = true, value = "")
  @NotNull
  public Song getSong() {
    return song;
  }

  public void setSong(Song song) {
    this.song = song;
  }

  public QueueEntry userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * The user who put the song in the queue
   *
   * @return userName
   **/
  @JsonProperty("userName")
  @ApiModelProperty(required = true, value = "The user who put the song in the queue")
  @NotNull
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueueEntry queueEntry = (QueueEntry) o;
    return Objects.equals(this.song, queueEntry.song) &&
        Objects.equals(this.userName, queueEntry.userName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(song, userName);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueueEntry {\n");

    sb.append("    song: ").append(toIndentedString(song)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first
   * line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

