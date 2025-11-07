Feature: Test Case: C40 --- TC01_LOGIN_Function

@TestRail @CaseID_40
Scenario Outline: TC01_LOGIN_Function
  Given User opens the "<loginURL>" page
  When User enters valid credentials "<username>" and "<password>"
  Then User should be logged in successfully

Examples:
  | loginURL | username | password | 
  | https://idp.bits-pilani.ac.in/idp/Authn/UserPassword | 2021hx70001@wilp.bits-pilani.ac.in | Pravallika@2001 | 
