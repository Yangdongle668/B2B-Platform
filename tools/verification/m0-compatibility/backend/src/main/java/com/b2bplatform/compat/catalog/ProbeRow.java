package com.b2bplatform.compat.catalog;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("probe_document")
public class ProbeRow {
    @TableId private Long id;
    private Long siteId;
    private String title;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
