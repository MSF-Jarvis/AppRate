# AppRate

[![GitHub workflow](https://github.com/msfjarvis/AppRate/workflows/CI%20builds/badge.svg)](https://github.com/msfjarvis/AppRate/actions)
[![Jitpack](https://jitpack.io/v/msfjarvis/AppRate.svg)](https://jitpack.io/#msfjarvis/AppRate)

* AppRate allows your users to rate your application.

* AppRate shows a customizable rate dialog according to your chosen settings.

* This fork of the library has been reworked to show consistent Material Design dialogs down to API level 8

* Also featured on the [CodePath](https://github.com/codepath/android_guides/wiki/Implementing-a-Rate-Me-Feature) Android Guides

## How to install and use

* Put this in your root `build.gradle` file

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

add the dependency

```gradle
dependencies {
    implementation 'com.github.msfjarvis:AppRate:1.3'
}
```

* Use AppRate as follows in your `MAIN` activity:

```java
new AppRate(this).init();
```

## Features

* You can decide **not to prompt the user** if the application **has crashed once**.

```java
new AppRate(this)
    .setShowIfAppHasCrashed(false)
    .init();
```

* You can decide **when to prompt the user**.

```java
new AppRate(this)
    .setMinDaysUntilPrompt(7)
    .setMinLaunchesUntilPrompt(20)
    .init();
```

* You can **customize** all the messages and buttons of **the rate dialog**.

```java
AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this)
    .setCustomTitle(myCustomTitleView)
    .setIcon(R.drawable.my_custom_icon)
    .setMessage("My custom message")
    .setPositiveButton("My custom positive button", null)
    .setNegativeButton("My custom negative button", null)
    .setNeutralButton("My custom neutral button", null);

new AppRate(this)
    .setCustomDialog(builder)
    .init();
```

* You can set **your own click listener**.

```java
new AppRate(this)
    .setOnClickListener(new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Do something.
        }
    })
    .init();
```

## License

This content is released under the MIT License.
