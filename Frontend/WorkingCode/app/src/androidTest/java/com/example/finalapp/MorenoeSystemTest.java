package com.example.finalapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import android.os.SystemClock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MorenoeSystemTest {
    
    private static final String TEST_USERNAME = "m";
    private static final String TEST_PASSWORD = "1";
    
    @Rule
    public ActivityScenarioRule<LoginActivity> activityScenarioRule = 
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmptyFieldValidation() {
        onView(withId(R.id.btnLogin)).perform(click());
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
    }

    @Test
    public void testValidLogin() {
        onView(withId(R.id.etUsername))
            .perform(typeText(TEST_USERNAME), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
            .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        SystemClock.sleep(2000);
    }

    @Test
    public void testInvalidLogin() {
        onView(withId(R.id.etUsername))
            .perform(typeText("wrong"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
            .perform(typeText("wrong"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        SystemClock.sleep(2000);
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
    }

    @Test
    public void testSignUpNavigation() {
        onView(withId(R.id.tvSignUp)).perform(click());
        SystemClock.sleep(1000);
    }

    @Test
    public void testForgotPasswordNavigation() {
        onView(withId(R.id.btnForgotPassword)).perform(click());
        SystemClock.sleep(1000);
    }

    @Test
    public void testSignUpProcess() {
        onView(withId(R.id.tvSignUp)).perform(click());
        SystemClock.sleep(1000);

        onView(withId(R.id.etFirstName))
            .perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.etLastName))
            .perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etUsername))
            .perform(typeText("testuser" + System.currentTimeMillis()), closeSoftKeyboard());
        onView(withId(R.id.etEmail))
            .perform(typeText("test" + System.currentTimeMillis() + "@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEdt))
            .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.etAge))
            .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.etGender))
            .perform(typeText("Male"), closeSoftKeyboard());

        onView(withId(R.id.btnSignUp)).perform(click());
        SystemClock.sleep(2000);
    }

    @Test
    public void testForgotPasswordProcess() {
        onView(withId(R.id.btnForgotPassword)).perform(click());
        SystemClock.sleep(1000);

        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.btnResetPassword)).perform(click());
        SystemClock.sleep(2000);
    }
}