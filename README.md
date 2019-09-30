# Android CameraX

CameraX aims to demonstrate how to use CameraX APIs written in Kotlin.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" 
alt="Get it on Google Play" height="90">](https://play.google.com/store/apps/details?id=com.arindam.camerax)

[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=102)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## Overview

CameraX is a Jet-pack support library, built to help you make camera app development easier. It 
provides a consistent and easy-to-use API surface that works across most Android devices, with 
backward-compatibility to Android 5.0 (API level 21).

- Ease of use
- Consistency across devices
- New camera experiences

While it leverages the capabilities of camera2, it uses a simpler, uses a case-based approach that 
is lifecycle-aware. It also resolves device compatibility issues for you so that you don't have to 
include device-specific code in your codebase. These features reduce the amount of code you need 
to write when adding camera capabilities to your app.

Lastly, CameraX enables developers to leverage the same camera experiences and features that 
pre-installed camera apps provide, with as little as two lines of code. CameraX Extensions are 
optional add-ons that enable you to add effects like Portrait, HDR, Night, and Beauty within your 
application on supported devices.

## Build

To build the app directly from the command line, run:
```sh
./gradlew assembleDebug
```

## Test

Unit testing and instrumented device testing share the same code. To test the app using Roboelectric, no device required, run:
```sh
./gradlew test
```

To run the same tests in an Android device connected via ADB, run:
```sh
./gradlew connectedAndroidTest
```

Alternatively, test running configurations can be added to Android Studio for convenience (and a nice UI). To do that:
1. Go to: `Run` > `Edit Configurations` > `Add New Configuration`.
1. For Roboelectric select `Android JUnit`, for connected device select `Android Instrumented Tests`.
1. Select `app` module and `com.arindam.camerax.MainInstrumentedTest` class.
1. Optional: Give the run configuration a name, like `test roboelectric` or `test device`

### Find this project useful ? :heart:
> Support it by clicking the :star: button on the upper right of this page. :v:

### TODO

> Implement photo editor, live filters and face detection.
> Add many more features and bug fixes.

### Contact - Let's become friends

- [Twitter](https://twitter.com/arindamxd)
- [Linkedin](https://in.linkedin.com/in/arindamxd)
- [GitHub](https://github.com/arindamxd)

### License

```
   Copyright (C) 2019 Arindam Karmakar, Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

### Contributing to Advanced Android Training

All pull requests are welcome, make sure to follow the [contribution guidelines](CONTRIBUTING.md) when you submit pull request.