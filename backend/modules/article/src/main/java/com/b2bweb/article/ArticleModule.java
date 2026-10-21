package com.b2bweb.article;
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_article")
public class ArticleModule implements com.b2bweb.api.extension.PlatformModule {
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public ArticleModule(@org.springframework.beans.factory.annotation.Qualifier("articleModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){this.descriptor=descriptor;}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){return descriptor;}
}
