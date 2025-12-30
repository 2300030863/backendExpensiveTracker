package com.expensetracker.dto;

public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean isDefault;

    public CategoryDto() {}

    public CategoryDto(Long id, String name, String description, String color, Boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.isDefault = isDefault;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}


