package com.b2bweb.core.support.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;

public abstract class BasePO {
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    this.id = value;
  }

  @TableField(fill = FieldFill.INSERT)
  private Instant createdAt;

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    this.createdAt = value;
  }

  @TableField(fill = FieldFill.INSERT)
  private Long createdBy;

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long value) {
    this.createdBy = value;
  }

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    this.updatedAt = value;
  }

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Long updatedBy;

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long value) {
    this.updatedBy = value;
  }
}
