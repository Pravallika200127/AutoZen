package locators;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class PageLocators {
    
    // ==================== Login/Authentication ====================
    public static final By LOGIN_USERNAME_INPUT = By.id("usernames");
    public static final By LOGIN_PASSWORD_INPUT = By.id("password");
    public static final By LOGIN_SUBMIT_BUTTON = By.id("submitbtn");

    // ==================== Top Navigation Bar ====================
    public static final By BITS_LOGO = By.xpath("//a[@href='/']//img");
    public static final By STUDENT_NAME = By.xpath("//span[@class='mx-1 text-black']");

    // ==================== Banner Section ====================
    public static final By BANNER_SECTION = By.xpath("//div[@class='container-fluid p-0 m-0']");

    // ==================== Dashboard Tiles (by data-tile attribute) ====================
    public static final By MY_ACADEMICS_TILE = By.xpath("(//li[@class='active']//div//div//h5)[1]");
    public static final By EXAMINATIONS_TILE = By.xpath("(//li[@class='active']//div//div//h5)[2]");
    public static final By STUDENT_SERVICES_TILE = By.xpath("(//li[@class='active']//div//div//h5)[3]");
    public static final By WILP_POLICIES_TILE = By.xpath("(//li[@class='active']//div//div//h5)[4]");
    public static final By STUDENT_SUPPORT_TILE = By.xpath("(//li[@class='active']//div//div//h5)[5]");
    public static final By MY_COURSES_TITLE = By.xpath("//h3[normalize-space()='My Courses']");
   
    

    // ==================== Buttons/CTAs ====================
    public static final By DETAILS_CTA_ON_ACADEMICS = By.xpath("//a[@class='card-text btn btn-sm w-100 btn-outline-primary' and @href='/academics/']");
    public static final By VIEW_COURSES_CTA = By.xpath("//a[@class='link-underline link-underline-opacity-0 link-light']");
    public static final By VIVA_PROJECT_SECTION_BUTTON = By.xpath("//button[contains(text(),'Viva/Project')]");
    public static final By GO_TO_VIVA_PROJECT_CTA = By.xpath("//button[contains(text(),'Go to Viva/Project portal')]");
    public static final By ELIBRARY_SECTION_BUTTON = By.xpath("(//a[@class='btn btn-primary'])[2]");
    public static final By VIRTUAL_LABS_SECTION= By.xpath("//section[@id='myVirtualLabs']");

    // ==================== Sections and Courses ====================
    public static final By AVAILABLE_COURSES_TITLE = By.xpath("//h5[normalize-space()='Dissertation (S2-24_SEHEXZG628T)']");
    public static final By COURSE_DETAILS_URL = By.xpath("//a[contains(@href,'CoursedetailsURL')]");
    public static final By COURSE_TITLE = By.xpath("//h1[@class='heding h2 iscurse maincoursepage']//a");

    // ==================== Misc/Status ====================
    public static final By DISSERTATION_STATUS = By.xpath("//h1[normalize-space()='Dissertation Evaluation Progress and Status']");
    public static final By PROJECT_URL_LINK = By.xpath("//a[contains(text(),'Project URL')]");

    // ==================== Dynamic Locators (Helper Methods) ====================
    
    /**
     * Get locator for element by text
     */
    public static By elementByText(String text) {
        return By.xpath("//*[contains(text(),'" + text + "')]");
    }
    
    /**
     * Get locator for link by text
     */
    public static By linkByText(String linkText) {
        return By.xpath("//a[contains(text(),'" + linkText + "')]");
    }
    
    /**
     * Get locator for button by text
     */
    public static By buttonByText(String buttonText) {
        return By.xpath("//button[contains(text(),'" + buttonText + "')]");
    }
    
    /**
     * Get locator for tile by data-tile attribute
     */
    public static By tileByDataAttribute(String dataValue) {
        return By.xpath("//div[@data-tile='" + dataValue + "']");
    }
    
    /**
     * Get locator for CTA button on specific tile
     */
    public static By ctaOnTile(String tileName, String ctaText) {
        String dataValue = mapTileNameToDataValue(tileName);
        return By.xpath("//div[@data-tile='" + dataValue + "']//button[contains(text(),'" + ctaText + "')]");
    }
    
    /**
     * Get locator for generic CTA button
     */
    public static By ctaButton(String ctaText) {
        return By.xpath("//button[contains(text(),'" + ctaText + "')]");
    }
    
    /**
     * Get locator for CTA link
     */
    public static By ctaLink(String ctaText) {
        return By.xpath("//a[contains(text(),'" + ctaText + "')]");
    }
    
    /**
     * Get locator for section by button text
     */
    public static By sectionButton(String sectionName) {
        return By.xpath("//button[contains(text(),'" + sectionName + "')]");
    }
    
    /**
     * Get locator for section by data attribute
     */
    public static By sectionByDataAttribute(String sectionName) {
        return By.xpath("//section[@data-section='" + sectionName + "'] | //div[@data-section='" + sectionName + "']");
    }
    
    /**
     * Get locator for profile icon by name
     */
    public static By profileIcon(String iconName) {
        return By.xpath("//div[contains(@class,'profile')]//i[contains(@class,'" + iconName.toLowerCase() + "')]");
    }
    
    /**
     * Get locator for course title
     */
    public static By courseTitle(String title) {
        return By.xpath("//h1[contains(text(),'" + title + "')]");
    }
    
    /**
     * Get locator for heading by level and text
     */
    public static By headingByText(int level, String text) {
        return By.xpath("//h" + level + "[contains(text(),'" + text + "')]");
    }
    
    /**
     * Get locator for input field by label
     */
    public static By inputByLabel(String labelText) {
        return By.xpath("//label[contains(text(),'" + labelText + "')]/following-sibling::input | " +
                       "//label[contains(text(),'" + labelText + "')]/..//input");
    }
    
    /**
     * Get locator for table cell by column and row
     */
    public static By tableCellByPosition(int row, int column) {
        return By.xpath("//table//tr[" + row + "]//td[" + column + "]");
    }
    
    /**
     * Get locator for element within parent by class
     */
    public static By elementInParent(String parentClass, String childTag) {
        return By.xpath("//div[contains(@class,'" + parentClass + "')]//" + childTag);
    }
    
    // ==================== Helper Method for Mapping ====================
    
    /**
     * Map user-friendly tile name to data-tile attribute value
     */
    private static String mapTileNameToDataValue(String tileName) {
        switch(tileName.toLowerCase()) {
            case "my academics":
                return "myAcademics";
            case "examinations":
                return "examinations";
            case "student services":
                return "studentServices";
            case "wilp policies":
                return "wilpPolicies";
            case "student support":
                return "studentSupport";
            case "my courses":
                return "Mycourses";
            default:
                return tileName.toLowerCase().replace(" ", "");
        }
    }
}