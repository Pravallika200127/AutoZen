package locators;

import org.openqa.selenium.By;

/**
 * ✅ Updated PageLocators - All Web Elements
 * Centralized locator management with static and dynamic methods
 */
public class PageLocators {
    
    // ==================== Login/Authentication ====================
    public static final By LOGIN_USERNAME_INPUT = By.id("username");
    public static final By LOGIN_PASSWORD_INPUT = By.id("password");
    public static final By LOGIN_SUBMIT_BUTTON = By.id("submitbtn");
    public static final By LOGIN_ERROR_MESSAGE = By.xpath("//div[contains(@class, 'error-message') or contains(@class, 'alert-danger')]");
    public static final By LOGIN_FORGOT_PASSWORD_LINK = By.xpath("//a[contains(text(), 'Forgot Password')]");

    // ==================== Top Navigation Bar ====================
    public static final By BITS_LOGO = By.xpath("//a[@href='/']//img");
    public static final By STUDENT_NAME = By.xpath("//span[@class='mx-1 text-black']");
    public static final By TOP_NAV_HOME = By.xpath("//nav//a[contains(text(), 'Home')]");
    public static final By TOP_NAV_DASHBOARD = By.xpath("//nav//a[contains(text(), 'Dashboard')]");
    public static final By TOP_NAV_PROFILE = By.xpath("//nav//a[contains(text(), 'Profile')]");
    public static final By PROFILE_ICON = By.xpath("//div[contains(@class,'profile-icon')] | //i[contains(@class,'profile')]");
    public static final By PROFILE_DROPDOWN = By.xpath("//div[@class='profile-dropdown'] | //ul[contains(@class,'dropdown-menu')]");
    public static final By LOGOUT_BUTTON = By.xpath("//button[contains(text(),'Logout')] | //a[contains(text(),'Logout')]");

    // ==================== Banner Section ====================
    public static final By BANNER_SECTION = By.xpath("//div[@class='container-fluid p-0 m-0']");
    public static final By BANNER_CAROUSEL = By.xpath("//div[contains(@class, 'carousel')] | //div[contains(@class, 'slider')]");
    public static final By BANNER_NEXT_BUTTON = By.xpath("//button[contains(@class,'carousel-control-next')] | //button[contains(@class,'slider-next')]");
    public static final By BANNER_PREV_BUTTON = By.xpath("//button[contains(@class,'carousel-control-prev')] | //button[contains(@class,'slider-prev')]");

    // ==================== Dashboard Tiles (by data-tile attribute) ====================
    public static final By MY_ACADEMICS_TILE = By.xpath("(//li[@class='active']//div//div//h5)[1]");
    public static final By EXAMINATIONS_TILE = By.xpath("(//li[@class='active']//div//div//h5)[2]");
    public static final By STUDENT_SERVICES_TILE = By.xpath("(//li[@class='active']//div//div//h5)[3]");
    public static final By WILP_POLICIES_TILE = By.xpath("(//li[@class='active']//div//div//h5)[4]");
    public static final By STUDENT_SUPPORT_TILE = By.xpath("(//li[@class='active']//div//div//h5)[5]");
    public static final By MY_COURSES_TITLE = By.xpath("//h3[normalize-space()='My Courses']");
    
    // Tile Container
    public static final By TILE_CONTAINER = By.xpath("//div[contains(@class, 'tile-container')] | //div[contains(@class, 'card-container')]");

    // ==================== Buttons/CTAs ====================
    public static final By DETAILS_CTA_ON_ACADEMICS = By.xpath("//a[@class='card-text btn btn-sm w-100 btn-outline-primary' and @href='/academics/']");
    public static final By VIEW_COURSES_CTA = By.xpath("//a[@class='link-underline link-underline-opacity-0 link-light']");
    public static final By VIVA_PROJECT_SECTION_BUTTON = By.xpath("//button[contains(text(),'Viva/Project')]");
    public static final By GO_TO_VIVA_PROJECT_CTA = By.xpath("//button[contains(text(),'Go to Viva/Project portal')]");
    public static final By ELIBRARY_SECTION_BUTTON = By.xpath("(//a[@class='btn btn-primary'])[2]");
    public static final By VIRTUAL_LABS_SECTION = By.xpath("//section[@id='myVirtualLabs']//div[@class='container-fluid py-5 px-3 bg-body']");

    // ==================== E-Library Section ====================
    public static final By ELIBRARY_SECTION = By.xpath("//section[@id='elibrary'] | //div[@data-section='elibrary']");
    public static final By ELIBRARY_VIEW_ALL_LINK = By.xpath("//a[contains(text(), 'View All Books')] | //a[contains(text(), 'View All')]");
    public static final By ELIBRARY_SEARCH_BOX = By.xpath("//input[@placeholder='Search books...'] | //input[@id='librarySearch']");
    public static final By ELIBRARY_BOOK_CARD = By.xpath("//div[contains(@class,'book-card')] | //div[contains(@class,'library-item')]");

    // ==================== Virtual Labs Section ====================
    public static final By VIRTUAL_LABS_TITLE = By.xpath("//h3[contains(text(),'Virtual Labs')] | //h2[contains(text(),'Virtual Labs')]");
    public static final By VIRTUAL_LABS_LIST = By.xpath("//div[@class='labs-list'] | //ul[@class='labs-list']");
    public static final By VIRTUAL_LABS_ITEM = By.xpath("//div[contains(@class,'lab-item')] | //li[contains(@class,'lab-item')]");

    // ==================== Viva/Project Section ====================
    public static final By VIVA_PROJECT_SECTION = By.xpath("//section[@id='viva-project'] | //div[@data-section='viva-project']");
    public static final By PROJECT_URL_LINK = By.xpath("//a[contains(text(),'Project URL')]");
    public static final By PROJECT_PORTAL_LINK = By.xpath("//a[contains(text(), 'Project Portal')] | //a[contains(@href,'project-portal')]");

    // ==================== Sections and Courses ====================
    public static final By AVAILABLE_COURSES_TITLE = By.xpath("//h5[normalize-space()='Dissertation (S2-24_SEHEXZG628T)']");
    public static final By COURSE_DETAILS_URL = By.xpath("//a[contains(@href,'CoursedetailsURL')]");
    public static final By COURSE_TITLE = By.xpath("//h1[@class='heding h2 iscurse maincoursepage']//a");
    public static final By COURSE_TILE = By.xpath("//div[contains(@class, 'course-tile')] | //div[contains(@class, 'course-card')]");
    public static final By COURSE_LIST = By.xpath("//div[@class='courses-list'] | //ul[@class='courses-list']");

    // ==================== Misc/Status ====================
    public static final By DISSERTATION_STATUS = By.xpath("//h1[normalize-space()='Dissertation Evaluation Progress and Status']");
    public static final By DASHBOARD_WELCOME_MESSAGE = By.xpath("//h1[contains(@class, 'welcome')] | //div[contains(@class,'welcome-message')]");
    public static final By PAGE_TITLE = By.xpath("//h1 | //h2[contains(@class,'page-title')]");
    public static final By LOADING_SPINNER = By.xpath("//div[contains(@class,'spinner')] | //div[contains(@class,'loading')]");
    public static final By SUCCESS_MESSAGE = By.xpath("//div[contains(@class,'alert-success')] | //div[contains(@class,'success-message')]");
    public static final By ERROR_MESSAGE = By.xpath("//div[contains(@class,'alert-danger')] | //div[contains(@class,'error-message')]");

    // ==================== Dynamic Locators (Helper Methods) ====================
    
    /**
     * Get locator for element by text (exact match or contains)
     */
    public static By elementByText(String text) {
        return By.xpath("//*[contains(text(),'" + text + "')]");
    }
    
    /**
     * Get locator for element by exact text
     */
    public static By elementByExactText(String text) {
        return By.xpath("//*[text()='" + text + "']");
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
        return By.xpath("//button[contains(text(),'" + buttonText + "')] | " +
                       "//input[@type='button' and contains(@value,'" + buttonText + "')] | " +
                       "//input[@type='submit' and contains(@value,'" + buttonText + "')]");
    }
    
    /**
     * Get locator for tile by data-tile attribute
     */
    public static By tileByDataAttribute(String dataValue) {
        return By.xpath("//div[@data-tile='" + dataValue + "']");
    }
    
    /**
     * Get locator for tile by name/title
     */
    public static By tileByName(String tileName) {
        return By.xpath("//div[contains(@class, 'tile') and contains(., '" + tileName + "')] | " +
                       "//div[contains(@class, 'card') and contains(., '" + tileName + "')]");
    }
    
    /**
     * Get locator for tile with specific value
     */
    public static By tileWithValue(String tileName, String value) {
        return By.xpath("//div[contains(@class, 'tile')]//h4[contains(text(), '" + tileName + "')]" +
                       "/following-sibling::*[contains(text(), '" + value + "')] | " +
                       "//div[contains(@class, 'card')]//h5[contains(text(), '" + tileName + "')]" +
                       "/following-sibling::*[contains(text(), '" + value + "')]");
    }
    
    /**
     * Get locator for CTA button on specific tile
     */
    public static By ctaOnTile(String tileName, String ctaText) {
        String dataValue = mapTileNameToDataValue(tileName);
        return By.xpath("//div[@data-tile='" + dataValue + "']//button[contains(text(),'" + ctaText + "')] | " +
                       "//div[@data-tile='" + dataValue + "']//a[contains(text(),'" + ctaText + "')] | " +
                       "//div[contains(., '" + tileName + "')]//button[contains(text(),'" + ctaText + "')] | " +
                       "//div[contains(., '" + tileName + "')]//a[contains(text(),'" + ctaText + "')]");
    }
    
    /**
     * Get locator for CTA in E-Library section
     */
    public static By ctaInElibrary(String ctaText) {
        return By.xpath("//section[@id='elibrary']//a[contains(text(), '" + ctaText + "')] | " +
                       "//section[@id='elibrary']//button[contains(text(), '" + ctaText + "')] | " +
                       "//div[@data-section='elibrary']//a[contains(text(), '" + ctaText + "')] | " +
                       "//div[@data-section='elibrary']//button[contains(text(), '" + ctaText + "')]");
    }
    
    /**
     * Get locator for CTA on course tile
     */
    public static By ctaOnCourseTile(String ctaText) {
        return By.xpath("//div[contains(@class, 'course-tile')]//a[contains(text(), '" + ctaText + "')] | " +
                       "//div[contains(@class, 'course-tile')]//button[contains(text(), '" + ctaText + "')] | " +
                       "//div[contains(@class, 'course-card')]//a[contains(text(), '" + ctaText + "')] | " +
                       "//div[contains(@class, 'course-card')]//button[contains(text(), '" + ctaText + "')]");
    }
    
    /**
     * Get locator for generic CTA button
     */
    public static By ctaButton(String ctaText) {
        return By.xpath("//button[contains(text(),'" + ctaText + "')] | " +
                       "//a[@role='button' and contains(text(),'" + ctaText + "')]");
    }
    
    /**
     * Get locator for CTA link
     */
    public static By ctaLink(String ctaText) {
        return By.xpath("//a[contains(text(),'" + ctaText + "')] | " +
                       "//a[contains(@title,'" + ctaText + "')]");
    }
    
    /**
     * Get locator for section by button text
     */
    public static By sectionButton(String sectionName) {
        return By.xpath("//button[contains(text(),'" + sectionName + "')] | " +
                       "//a[contains(@class,'section-link') and contains(text(),'" + sectionName + "')]");
    }
    
    /**
     * Get locator for section by name
     */
    public static By sectionByName(String sectionName) {
        String sectionId = sectionName.toLowerCase().replace(" ", "-").replace("/", "-");
        return By.xpath("//section[@id='" + sectionId + "'] | " +
                       "//div[@data-section='" + sectionId + "'] | " +
                       "//div[contains(@class, 'section') and contains(., '" + sectionName + "')] | " +
                       "//section[contains(., '" + sectionName + "')]");
    }
    
    /**
     * Get locator for section by data attribute
     */
    public static By sectionByDataAttribute(String sectionName) {
        return By.xpath("//section[@data-section='" + sectionName + "'] | " +
                       "//div[@data-section='" + sectionName + "']");
    }
    
    /**
     * Get locator for profile icon by name
     */
    public static By profileIcon(String iconName) {
        return By.xpath("//div[contains(@class,'profile')]//i[contains(@class,'" + iconName.toLowerCase() + "')] | " +
                       "//div[@class='profile-icon' and @title='" + iconName + "'] | " +
                       "//i[contains(@class,'fa-" + iconName.toLowerCase() + "')] | " +
                       "//span[contains(@class,'icon-" + iconName.toLowerCase() + "')]");
    }
    
    /**
     * Get locator for course title
     */
    public static By courseTitle(String title) {
        return By.xpath("//h1[contains(text(),'" + title + "')] | " +
                       "//h2[contains(text(),'" + title + "')] | " +
                       "//h3[@class='course-title' and contains(text(),'" + title + "')]");
    }
    
    /**
     * Get locator for heading by level and text
     */
    public static By headingByText(int level, String text) {
        return By.xpath("//h" + level + "[contains(text(),'" + text + "')]");
    }
    
    /**
     * Get locator for heading by text (any level)
     */
    public static By headingByText(String text) {
        return By.xpath("//h1[contains(text(), '" + text + "')] | " +
                       "//h2[contains(text(), '" + text + "')] | " +
                       "//h3[contains(text(), '" + text + "')] | " +
                       "//h4[contains(text(), '" + text + "')] | " +
                       "//h5[contains(text(), '" + text + "')] | " +
                       "//h6[contains(text(), '" + text + "')]");
    }
    
    /**
     * Get locator for input field by placeholder
     */
    public static By inputByPlaceholder(String placeholder) {
        return By.xpath("//input[@placeholder='" + placeholder + "']");
    }
    
    /**
     * Get locator for input field by label
     */
    public static By inputByLabel(String labelText) {
        return By.xpath("//label[contains(text(),'" + labelText + "')]/following-sibling::input | " +
                       "//label[contains(text(),'" + labelText + "')]/..//input | " +
                       "//label[contains(text(),'" + labelText + "')]/parent::*/input");
    }
    
    /**
     * Get locator for input field by name attribute
     */
    public static By inputByName(String name) {
        return By.name(name);
    }
    
    /**
     * Get locator for table cell by column and row
     */
    public static By tableCellByPosition(int row, int column) {
        return By.xpath("//table//tr[" + row + "]//td[" + column + "]");
    }
    
    /**
     * Get locator for table cell by text
     */
    public static By tableCellByText(String text) {
        return By.xpath("//table//td[contains(text(),'" + text + "')]");
    }
    
    /**
     * Get locator for element within parent by class
     */
    public static By elementInParent(String parentClass, String childTag) {
        return By.xpath("//div[contains(@class,'" + parentClass + "')]//" + childTag);
    }
    
    /**
     * Get locator for element by class name
     */
    public static By elementByClass(String className) {
        return By.className(className);
    }
    
    /**
     * Get locator for element by ID
     */
    public static By elementById(String id) {
        return By.id(id);
    }
    
    /**
     * Get locator for element by attribute
     */
    public static By elementByAttribute(String attributeName, String attributeValue) {
        return By.xpath("//*[@" + attributeName + "='" + attributeValue + "']");
    }
    
    /**
     * Get locator for element containing attribute
     */
    public static By elementContainingAttribute(String attributeName, String attributeValue) {
        return By.xpath("//*[contains(@" + attributeName + ",'" + attributeValue + "')]");
    }
    
    /**
     * Get locator for checkbox by label
     */
    public static By checkboxByLabel(String labelText) {
        return By.xpath("//label[contains(text(),'" + labelText + "')]/input[@type='checkbox'] | " +
                       "//label[contains(text(),'" + labelText + "')]/..//input[@type='checkbox']");
    }
    
    /**
     * Get locator for radio button by label
     */
    public static By radioByLabel(String labelText) {
        return By.xpath("//label[contains(text(),'" + labelText + "')]/input[@type='radio'] | " +
                       "//label[contains(text(),'" + labelText + "')]/..//input[@type='radio']");
    }
    
    /**
     * Get locator for dropdown by label
     */
    public static By dropdownByLabel(String labelText) {
        return By.xpath("//label[contains(text(),'" + labelText + "')]/following-sibling::select | " +
                       "//label[contains(text(),'" + labelText + "')]/..//select");
    }
    
    /**
     * Get locator for dropdown option by text
     */
    public static By dropdownOptionByText(String optionText) {
        return By.xpath("//option[contains(text(),'" + optionText + "')]");
    }
    
    /**
     * Get locator for icon by class
     */
    public static By iconByClass(String iconClass) {
        return By.xpath("//i[contains(@class,'" + iconClass + "')] | " +
                       "//span[contains(@class,'" + iconClass + "')]");
    }
    
    /**
     * Get locator for image by alt text
     */
    public static By imageByAlt(String altText) {
        return By.xpath("//img[@alt='" + altText + "' or contains(@alt,'" + altText + "')]");
    }
    
    /**
     * Get locator for element by data attribute (generic)
     */
    public static By elementByDataAttribute(String attributeName, String attributeValue) {
        return By.xpath("//*[@data-" + attributeName + "='" + attributeValue + "']");
    }
    
    /**
     * Get locator for first child element
     */
    public static By firstChild(String parentXpath) {
        return By.xpath("(" + parentXpath + ")[1]");
    }
    
    /**
     * Get locator for last child element
     */
    public static By lastChild(String parentXpath) {
        return By.xpath("(" + parentXpath + ")[last()]");
    }
    
    /**
     * Get locator for element by index
     */
    public static By elementByIndex(String baseXpath, int index) {
        return By.xpath("(" + baseXpath + ")[" + index + "]");
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
            case "e-library":
            case "elibrary":
                return "elibrary";
            case "virtual labs":
                return "virtualLabs";
            case "viva/project":
            case "viva project":
                return "vivaProject";
            default:
                return tileName.toLowerCase().replace(" ", "").replace("/", "");
        }
    }
    
    /**
     * Get user-friendly name from data-tile value
     */
    public static String getDisplayName(String dataValue) {
        switch(dataValue.toLowerCase()) {
            case "myacademics":
                return "My Academics";
            case "examinations":
                return "Examinations";
            case "studentservices":
                return "Student Services";
            case "wilppolicies":
                return "WILP Policies";
            case "studentsupport":
                return "Student Support";
            case "mycourses":
                return "My Courses";
            case "elibrary":
                return "E-Library";
            case "virtuallabs":
                return "Virtual Labs";
            case "vivaproject":
                return "Viva/Project";
            default:
                return dataValue;
        }
    }
}