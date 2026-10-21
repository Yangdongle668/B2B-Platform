package com.b2bweb.core.support.persistence;

import com.baomidou.mybatisplus.annotation.*;

public abstract class SitePO extends BasePO {
  @TableField(fill = FieldFill.INSERT)
  private Long siteId;

  public Long getSiteId() {
    return siteId;
  }

  public void setSiteId(Long value) {
    siteId = value;
  }
}
