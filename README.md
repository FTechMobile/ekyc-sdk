# [Android] Ekyc SDK
## 1. Install Ekyc SDK

#### Setup gradle maven:

* With Gradle v1.x - v5.x

  Open `build.grade` file and add maven line like below

``` groovy
allprojects {
    repositories {
        google()  
        mavenCentral()
        maven {
            url 'https://gitlab.com//api/v4/projects/40140113/packages/maven'
            allowInsecureProtocol true
            credentials(HttpHeaderCredentials) {
                name = "Private-Token"
                value = "{SDK_PRIVATE_KEY}"
            }
            authentication {
                header(HttpHeaderAuthentication)
            }
        }
    }
}

```

* With Gradle v6.x+

  Open `setting.gradle` file and add maven line like below

``` groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://gitlab.com//api/v4/projects/40140113/packages/maven'
            allowInsecureProtocol true
            credentials(HttpHeaderCredentials) {
                name = "Private-Token"
                value = "{SDK_PRIVATE_KEY}"
            }
            authentication {
                header(HttpHeaderAuthentication)
            }
        }
    }
}
```

#### Open file app/build.grade then add sdk:

``` groovy 
dependencies {
...
   implementation 'ai.ftech:ekyc:1.1.0'
}
```

#### Init Ekyc in file Application
``` java
override fun onCreate() {
        super.onCreate()
        ...
        FTechEkycManager.init(this)
    }
```

## 2. SDK Integration

#### Register callback
Calling functions in activity lifecycle

``` java
    @Override
    protected void onResume() {
        super.onResume();
        FTechEkycManager.notifyActive(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FTechEkycManager.notifyInactive(this);
    }
```

#### Register Ekyc
|Param|Type|Description|
|---|---|---|
|appId|String|Ekyc app id|
|licenseKey|String|Ekyc license key|
+ Upon successful registration, the SDK returns a status resulting in the `onSuccess()` callback.  Handling of successful registration here.
+ When register fails, it will be processed at callback `onFail()`.
``` java
FTechEkycManager.registerEkyc(appId, licenseKey, new IFTechEkycCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean info) {

            }

            @Override
            public void onFail(APIException error) {

            }

            @Override
            public void onCancel() {

            }
        });
```

## 3. SDK Feature

### Enable Liveness detection
For enable/disable liveness detection, use this:

``` java
 FTechEkycManager.setEnableLiveness(true);
``` 

#### Create transaction
+ Used to create transaction for Ekyc execution session
+ When successful transaction creation, the SDK returns a model transaction data which leads to the `onSuccess()` callback. Handling create transaction successfully here.
+ When creating transaction fails, it will be processed at callback `onFail()`.
``` java
FTechEkycManager.createTransaction(new IFTechEkycCallback<TransactionData>() {
            @Override
            public void onSuccess(TransactionData info) {

            }

            @Override
            public void onFail(APIException error) {

            }

            @Override
            public void onCancel() {

            }
        });
```

#### Get process transaction
+ Used to get the Ekyc process of a transaction

|Param|Type| Description    |
|---|---|----------------|
|transactionId|String| Transaction id |
+ When the get process transaction is successful, the SDK returns a model process transaction data resulting in the `onSuccess()` callback. Handling get process transaction successfully here.
+ When get process transaction fails, it will be processed at callback `onFail()`.
``` java
FTechEkycManager.getProcessTransaction(transactionId, new IFTechEkycCallback<TransactionProcessData>() {
            @Override
            public void onSuccess(TransactionProcessData info) {
                
            }

            @Override
            public void onFail(APIException error) {
                
            }

            @Override
            public void onCancel() {
                
            }
        });
```

#### Upload Photo (Normal detection)
+ Used to upload photos of documents, face for Ekyc

| Param       |Type| Description                                                                                              |
|-------------|---|----------------------------------------------------------------------------------------------------------|
| pathImage   |String| Image path local                                                                                         |
| captureType |CAPTURE_TYPE| Orientation images include the following types: CAPTURE_TYPE.FRONT, CAPTURE_TYPE.BACK, CAPTURE_TYPE.FACE |
+ When the photo upload is successful, the SDK returns a model capture data resulting in the `onSuccess()` callback. Handling photo upload successfully here.
+ When uploading photo fails, it will be handled at callback `onFail()`.
``` java
 FTechEkycManager.uploadPhoto(pathImage, captureType, new IFTechEkycCallback<CaptureData>() {
            @Override
            public void onSuccess(CaptureData info) {
            
            }

            @Override
            public void onFail(APIException error) {
            
            }

            @Override
            public void onCancel() {
            
            }
        });
```

#### Face Matching
+ Use this method to get ORC scan information

`FaceMatchingData`

|Param|Type| Description      |
|---|---|------------------|
|sessionId|String| Session id       |
|cardInfo|CardInfo| Card information |

`CardInfo`

|Param|Type| Description |
|---|---|--|
|id|String| id card |
|birthDay|String| birth day |
|birthPlace|String| birth place |
|cardType|String| card type |
|gender|String| gender |
|issueDate|String| issue date |
|issuePlace|String| issue place |
|name|String| full name |
|nationality|String| nationality |
|originLocation|String| origin location |
|passportNo|String| passport no |
|recentLocation|String| recent location |
|validDate|String| valid date |
|feature|String| feature|
|nation|String| nation |
|mrz|String| mrz |

+ When face matching is successful, the SDK returns a model face matching data resulting in the `onSuccess()` callback. Handling face matching successfully here.
+ When face matching fails, it will be handled at the callback `onFail()`.
``` java
       FTechEkycManager.faceMatching(new IFTechEkycCallback<FaceMatchingData>() {
           @Override
           public void onSuccess(FaceMatchingData info) {
           }

           @Override
           public void onFail(APIException error) {
           }

           @Override
           public void onCancel() {
           }
       })
```

### Liveness detection
+ Using bitmap image to check face position
```java
FTechEkycManager.detectFacePose(bitmap, FACE_POSE.UP, new IFTechEkycCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean info) {
                
            }

            @Override
            public void onFail(APIException error) {
          
            }

            @Override
            public void onCancel() {
               
            }
        });
```

#### Submit info
+ Use this method to submit information

`NewSubmitInfoRequest`

|Param|Type| Description      |
|---|---|------------------|
|cardInfoSubmit|CardInfoSubmit| Information card |
|preProcessId|String| Session id       |

`CardInfoSubmit`

|Param|Type| Description |
|---|---|--|
|id|String| id card |
|birthDay|String| birth day |
|birthPlace|String| birth place |
|cardType|String| card type |
|gender|String| gender |
|issueDate|String| issue date |
|issuePlace|String| issue place |
|name|String| full name |
|nationality|String| nationality |
|originLocation|String| origin location |
|passportNo|String| passport no |
|recentLocation|String| recent location |
|validDate|String| valid date |
|feature|String| feature|
|nation|String| nation |
|mrz|String| mrz |

+ After submitting the info successfully, the SDK returns a status leading to the `onSuccess()` callback.  Handling submit info successfully here.
+ When submitting information fails, it will be handled at the callback `onFail()`.
``` java
FTechEkycManager.submitInfo(submitInfoRequest, new IFTechEkycCallback<Boolean>() {
           @Override
           public void onSuccess(Boolean info) {
           }

           @Override
           public void onFail(APIException error) {
           }

           @Override
           public void onCancel() {
           }
       });
```