Feature: Test Case: C296 --- TC02_E2E_Validation_BITS

@TestRail @CaseID_296
Scenario Outline: TC02_E2E_Validation_BITS
  Given User opens the "<loginURL>" page
  When User enters valid credentials "<username>" and "<password>"
  Then User should be logged in successfully
  Given User verifies "Bits Logo and Name" is displaying in Top Navigation bar
  Then User verifies "Student name" is displaying in Top Navigation bar
  Then User verifies "Banner section" is displaying in with multiple banners in Dashboard
  Then User verifies "My Academics" as "<myAcademics>" Tile is displaying in Dashboard
  Then User verifies "Examinations" as "<examinations>" Tile is displaying in Dashboard
  Then User verifies "Student Services" as "<studentServices>" Tile is displaying in Dashboard
  Then User verifies "WILP Policies" as "<wilpPolicies>" Tile is displaying in Dashboard
  Then User verifies "Student Support" as "<studentSupport>" Tile is displaying in Dashboard
  When User Clicks on "Details" CTA on "My Academics" Tile in Dashboard
  Then User verifies "My Courses" as "<Mycourses>" is displaying in Dashboard
  Then User verifies "Available course" as "<availableCourse>" is displaying in Dashboard
  When User Clicks on "View Courses" CTA on Course Tile
  Then User verifies "<courseURL>" opened in new tab
  Then User verifies "Course title" as "<courseTitle>" in Subject Screen and closes the tab
  When User Clicks on "eLibrary" CTA in Elibrary Section
  Then User verifies "<elibraryURL>" redirecting in new tab and closed the tab
  Then User verifies "My virtual Labs" Sections and validate the available labs in Dashboard
  Then user verifies and clicks on "Go to Viva/Project portal" CTA
  Then User verifies "Project URL" opened in new tab
  Then User verifies "Dissertions Rating" as "<dissertationStatus>" in Subject Screen and closes the tab

Examples:
  | loginURL | username | password | myAcademics | examinations | studentServices | wilpPolicies | studentSupport | Mycourses | availableCourse | courseURL | courseTitle | elibraryURL | dissertationStatus | 
  | https://idp.bits-pilani.ac.in/idp/Authn/UserPassword | 2021hx70001@wilp.bits-pilani.ac.in | Pravallika@2001 | My Academics | Examinations | Student Services | WILP Policies | Student Support | My Courses | Dissertation (S2-24_SEHEXZG628T) | https://taxila-aws.bits-pilani.ac.in/course/view.php?id=15265 | Dissertation (S2-24_SEHEXZG628T) | https://my.openathens.net/?passiveLogin=false | Dissertation Evaluation Progress and Status | 
