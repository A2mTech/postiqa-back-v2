package fr.postiqa.core.domain.enums;

/**
 * Types of web content that can be scraped
 */
public enum ContentType {
    BLOG_ARTICLE("Blog Article", "article, blog post, news"),
    PRODUCT_PAGE("Product Page", "e-commerce product, item listing"),
    ABOUT_PAGE("About Page", "company profile, about us, team page");

    private final String displayName;
    private final String description;

    ContentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
