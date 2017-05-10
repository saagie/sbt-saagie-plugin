# sbt-saagie-plugin

[![Build Status](https://travis-ci.org/saagie/sbt-saagie-plugin.svg?branch=master)](https://travis-ci.org/saagie/sbt-saagie-plugin)

A SBT plugin to push jar in Saagie Manager and create a new Scala or Spark job

More informations about Saagie : https://www.saagie.com/

* [Usage](#usage)
  * [Creation](#creation)
  * [Update](#update)
* [Configuration](#configuration)
  * [List of all parameters available](#list-of-all-parameters-available)
    * [Mandatory for all modes](#mandatory-for-all-modes)
    * [Mandatory for update mode only](#mandatory-for-update-mode-only)
    * [Mandatory if you use authenticating directly in build.sbt](#mandatory if you use authenticating directly in build.sbt)
  * [Others parameters can be overriden](#others-parameters-can-be-overriden)
    
## Usage

### Creation
You can create a job with the above configurations by running this command.

    sbt createSaagieJob

In order for this to succeed, at least some parameters can be set.

    urlApi in createSaagieJob := "https://manager.prod.saagie.io/api/v1"
    login in createSaagieJob := "your-login"
    password in createSaagieJob := "your-password"
    platformId in createSaagieJob:= "your-platform"
    jobName in createSaagieJob:= "your-job-name"
    jobType in createSaagieJob := "java-scala"
    jobCategory in createSaagieJob := "extract"
    targetDirectory in createSaagieJob :="your-target-directory"
    jar in createSaagieJob := "your-jar-name"
    mainClazz in createSaagieJob := "your-main-class"
        
### Update
You can update a job with the above configurations by running this command.

    sbt updateSaagieJob

In order for this to succeed, at least some parameters can be set.

     urlApi in createSaagieJob := "https://manager.prod.saagie.io/api/v1"
     login in createSaagieJob := "your-login"
     password in createSaagieJob := "your-password"
     platformId in createSaagieJob:= "your-platform"
     jobName in createSaagieJob:= "your-job-name"
     jobType in createSaagieJob := "java-scala"
     jobCategory in createSaagieJob := "extract"
     targetDirectory in createSaagieJob :="your-target-directory"
     jar in createSaagieJob := "your-jar-name"
     mainClazz in createSaagieJob := "your-main-class"
     jobId in createSaagieJob := 0

The difference between create and update is the add of the "jobId" parameter in the configuration to know which job should be updated (if present in the "create" mode, it'll be just ignored)

## Configuration

### List of all parameters available

#### Mandatory for all modes
These parameters are mandatory (in create and update mode) :

* **platformId**
  - represents the id of the platform you want to add the job. This id is accessible via the URL when you are authenticated and in your manager : _https://.../#/manager/**1**_ - Here "**1**" is the plaformId
* **jobName**
  - represents the name of the job you want to create or to update (should be exactly the same for update - a verification is made)
* **jobCategory**
  - represents the category of the job you want to create or to update (should be exactly the same for update - a verification is made).
  - can be : "**extract**" or "**processing**" (another values can produce errors)
* **jobType**
  - represents the type of the job you want to create or to update (should be exactly the same for update - a verification is made).
  - can be : "**java-scala**" or "**spark**" (another values can produce errors)
* **targetDirectory**
  - represents the name of the jar you want to upload
* **jarName**
  - represents the path for finding your jar
  
#### Mandatory for update mode only 

* **jobId**
  - represents the id the job you want to update. This id is accessible via the URL when you are on the details page of the job : _https://.../#/manager/1/job/**49**_ Here "**49**" is the jobId. 

#### Mandatory if you use authenticating directly in build.sbt  

We recommand to use the authenticating mode using the ~/.sbt/0.13/global.sbt. It's more secure and you'll be sure to never commit your login/password in your build.sbt.

* **login** 
  - represents the login you'll use to have access to your manager (UI and API use the same)

* **password** 
  - represents the password you'll use to have access to your manager (UI and API use the same)
  
  
### Others parameters can be overriden

* **urlAPI**
  - represents the URL of your manager
  - default value : "https://manager.prod.saagie.io/api/v1" (for Saagie Kumo)
  - you can override this parameter if you use a Saagie Su (Appliance). Don't forget to add "**/api/v1**" at the end of the URL

* **cpu / mem / disk**
  - represents the amount of CPU / memory / disk space you want to reserve for your job (like you can set in the manager interface)
  - default values : **cpu**:0.5 / **mem**:512 / **disk**:1024
  - for **cpu**, the value represents the number of core (0.5 represent an half of core)
  - for **mem** and **disk**, the value represents the number of mega-octet allocated
  
* **javaVersion**
  - represents the version of language you want to run your job
  - default value : 8
  - only 8 or 7 are available (See [anapsix/alpine-java](https://hub.docker.com/r/anapsix/alpine-java/) to see the precise version of java we use). 

* **sparkVersion**
  - represents the version of spark you want to run your job
  - default value : 1.6.1
  
* **streaming**
  - represents if the job is a streaming job or not
  - default value : false

* **arguments**
  - represents the arguments in the the job command-line
  - default value : Empty-String

* **jobType**
  - represents the type of job you want to create
  - default value : "java-scala"

* **description**
  - represents the description of job you want to create

* **releaseNote**
  - represents the release note of the job version you want to create / update
