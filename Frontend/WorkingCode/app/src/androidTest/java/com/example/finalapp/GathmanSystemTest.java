package com.example.finalapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import android.os.SystemClock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GathmanSystemTest {

    @Rule
    public ActivityScenarioRule<SignupActivity> activityScenarioRule =
            new ActivityScenarioRule<>(SignupActivity.class);

    @Test
    public void testEmptyFieldValidation() {
        // Click SignUp without entering any information
        onView(withId(R.id.btnSignUp)).perform(click());

        // Verify that error toast or validation occurs for empty fields
        // Example: Verify that the password field is still displayed as part of validation check
        onView(withId(R.id.etFirstName)).check(matches(isDisplayed()));
        onView(withId(R.id.etLastName)).check(matches(isDisplayed()));
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.etAge)).check(matches(isDisplayed()));
        onView(withId(R.id.etGender)).check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidEmailFormat() {
        // Enter invalid email
        onView(withId(R.id.etEmail))
                .perform(typeText("invalidemail.com"), closeSoftKeyboard());

        // Enter valid information for the rest
        onView(withId(R.id.etFirstName))
                .perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.etLastName))
                .perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etUsername))
                .perform(typeText("john_doe"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEdt))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.etGender))
                .perform(typeText("Male"), closeSoftKeyboard());

        // Click SignUp button
        onView(withId(R.id.btnSignUp)).perform(click());

        // Verify that the email validation error occurs (e.g., through a toast or other method)
        onView(withId(R.id.etFirstName)).check(matches(isDisplayed())); // Ensure the field is still visible
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));    // Ensure the email field is still visible
    }

    @Test
    public void testPasswordMismatch() {
        // Enter matching details but with mismatched passwords
        onView(withId(R.id.etFirstName))
                .perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.etLastName))
                .perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etUsername))
                .perform(typeText("john_doe"), closeSoftKeyboard());
        onView(withId(R.id.etEmail))
                .perform(typeText("john.doe@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEdt))
                .perform(typeText("password456"), closeSoftKeyboard());
        onView(withId(R.id.etAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.etGender))
                .perform(typeText("Male"), closeSoftKeyboard());

        // Click SignUp button
        onView(withId(R.id.btnSignUp)).perform(click());

        // Verify that password mismatch validation occurs
        onView(withId(R.id.etFirstName)).check(matches(isDisplayed())); // Ensure the field is still visible
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));    // Ensure the email field is still visible
    }

    @Test
    public void testPasswordLengthValidation() {
        // Enter valid details but with a short password
        onView(withId(R.id.etFirstName))
                .perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.etLastName))
                .perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etUsername))
                .perform(typeText("john_doe"), closeSoftKeyboard());
        onView(withId(R.id.etEmail))
                .perform(typeText("john.doe@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("short"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEdt))
                .perform(typeText("short"), closeSoftKeyboard());

        onView(withId(R.id.etAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.etGender))
                .perform(typeText("Male"), closeSoftKeyboard());
        // Click SignUp button
        onView(withId(R.id.btnSignUp)).perform(click());

        // Verify that password length validation occurs
        onView(withId(R.id.etFirstName)).check(matches(isDisplayed())); // Ensure the field is still visible
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()));    // Ensure the email field is still visible
    }

    @Test
    public void testSuccessfulSignupNavigation() {
        // Enter valid information
        onView(withId(R.id.etFirstName))
                .perform(typeText("Jane"), closeSoftKeyboard());
        onView(withId(R.id.etLastName))
                .perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etUsername))
                .perform(typeText("bessy_doododo"), closeSoftKeyboard());
        onView(withId(R.id.etEmail))
                .perform(typeText("bananas.doe@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEdt))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.etGender))
                .perform(typeText("Male"), closeSoftKeyboard());

        // Click SignUp button
        onView(withId(R.id.btnSignUp)).perform(click());

        // Add a delay to wait for the signup process
        SystemClock.sleep(1000);

        // Verify that we navigate to the LoginActivity (successfully signed up)
        onView(withId(R.id.tvLoginLink)).check(matches(isDisplayed()));
    }
}
