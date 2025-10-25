# diff-check

[![Java CI with Maven](https://github.com/yangziwen/diff-check/actions/workflows/maven.yml/badge.svg)](https://github.com/yangziwen/diff-check/actions/workflows/maven.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/0101bd3b974d4578813339011f07fcb7)](https://app.codacy.com/gh/yangziwen/diff-check/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

[Chinese Doc](https://github.com/yangziwen/diff-check/blob/master/README_CN.md)
### Introduction
When using code analysis tools to scan a project, it will output all the problems in each file at once. This makes it difficult to perform an effective check on the codes under development before cleaning up all the existing problems.

In response to this pain point, I extended those tools including ***checkstyle***, ***PMD*** and ***jacoco*** to perform scanning only on incremental changes of code lines.

### Usage
#### Run the style and defect check when submitting a commit to your repository
* You can run the following commands to install the check tools
```Shell
# Download the install.sh
 curl https://raw.githubusercontent.com/yangziwen/diff-check/master/hooks/install.sh > install.sh

# Install the hook to a specified git repository
 sh install.sh --repo-path=${the_absolute_path_of_your_git_repository}

# Or install the hook globally
 sh install.sh --global=true
```

* You can enable or disable the check in the following way
```Shell
# Enable the check of checkstyle
 git config diff-check.checkstyle.enabled true
 
 # Disable the check of checkstyle
 git config diff-check.checkstyle.enabled false
 
 # Enable the check of PMD
 git config diff-check.pmd.enabled true
 
 # Disable the check of PMD
 git config diff-check.pmd.enabled false
```

* You can set the check rules in the following way
```Shell
 git config diff-check.checkstyle.config-file /custom_checks.xml
 
 git config diff-check.pmd.rulesets rulesets/java/ali-all.xml
```

* You can set the exampt path in the following way
```Shell
 git config diff-check.checkstyle.exclude-regexp .+-client/.*
 
 git config diff-check.pmd.exclude-regexp .+-client/.*
```

* You can choose language in the following way 
  (English is the default choice if the language you specified is not supported)
```Shell
# Specify the message language of both checkstyle and pmd
git config diff-check.language en

# Only specify the message language of checkstyle
git config diff-check.checkstyle.language es

# Only specify the message language of pmd
git config diff-check.pmd.language zh

```
* Be aware that you can make all the  `git config` commands effective globally by add [`--global`](https://git-scm.com/docs/git-config) option.

#### Run unit tests and get the code coverage report

* Config the maven plugin
```Xml
 <plugin>
     <groupId>io.github.yangziwen</groupId>
     <artifactId>diff-jacoco-maven-plugin</artifactId>
     <version>0.0.8</version>
     <configuration>
       <excludes>
         <!-- Specify the path to exclude -->
         <exclude>io/github/yangziwen/quickdao/example/entity/*</exclude>
       </excludes>
     </configuration>
     <executions>
         <execution>
             <id>pre-test</id>
             <goals>
                 <goal>prepare-agent</goal>
             </goals>
         </execution>
         <execution>
             <id>post-test</id>
             <phase>test</phase>
             <goals>
                 <goal>report</goal>
             </goals>
             <configuration>
                 <!-- config the path of the report -->
                 <outputDirectory>${project.reporting.outputDirectory}/jacoco-diff</outputDirectory>
             </configuration>
         </execution>
     </executions>
 </plugin>
```

* Run the unit tests
```Shell
 # Incremental code coverage scanning is achieved by specifying commit or branch for `jacoco.diff.oldrev` and `jacoco.diff.newrev`
 # If these two parameters are not specified, a full coverage scan will be performed
 mvn test -Djacoco.diff.oldrev=9ac9182 -Djacoco.diff.newrev=HEAD
 
 # Use `jacoco.diff.against` parameter，you can compare the difference between HEAD and any other commit or branch, and get the code coverage result between them. 
 mvn test -Djacoco.diff.against=origin/master

 # Use the `jacoco.author.name` or `jacoco.author.email` parameters to complete the coverage scanning according to the incremental code of the specified submitter
 mvn test -Djacoco.diff.against=origin/master -Djacoco.author.name=yangziwen
 
 # You can run a single test class and get the report of it
 mvn clean test -Djacoco.diff.against=origin/master -DskipTests=false -Dtest=io.github.yangziwen.quickdao.example.repository.UserSpringJdbcRepositoryTest -DfailIfNoTests=false
```

* Get the result

Full Code Coverage | Incremental Code Coverage
-|-
![全量覆盖率概览](https://github.com/user-attachments/assets/4d4d15db-81f0-4f1c-83b8-5cac751c4b19) | ![增量覆盖率概览](https://github.com/user-attachments/assets/c106fb48-0ead-4698-b23f-98cf599f1e75)
![全量覆盖率详情](https://github.com/user-attachments/assets/4954b7a3-7c4a-4c33-a0f6-bf9b5482fd4b) | ![增量覆盖率详情](https://github.com/user-attachments/assets/cf874bec-6400-448b-b7fd-33478c283ee8)

#### Integrate diff-check into Jenkins pipeline
* Install the following Jenkins plugins
    * Pipeline Plugin
    * Git Plugin
    * Code Coverage API Plugin
    * Warning Next Generation Plugin
* Setup the Jenkins Pipeline configuration (the following example is for demonstration purposes only)
```groovy
pipeline {  
    agent any
    tools {
        maven 'mvn-3.9.4'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'diff-check-test-branch', url: 'https://github.com/yangziwen/quick-dao'
            }
        }
        stage('Check Style') {
            steps {
                script {
                    sh "curl -L -o diff-checkstyle.jar https://github.com/yangziwen/diff-check/releases/download/0.0.8/diff-checkstyle.jar"
                    def returnStatus = sh returnStatus:true, script: "java -jar ./diff-checkstyle.jar -c /custom_checks.xml ${WORKSPACE} --git-dir ${WORKSPACE} --base-rev=origin/master -f xml -o ${WORKSPACE}/checkstyle-result.xml"
                    recordIssues tools: [checkStyle(name: 'Diff-CheckStyle', pattern: '**/checkstyle-result.xml', reportEncoding: 'UTF-8')]
                    if (returnStatus != 0) {
                        error("Diff-CheckStyle ends with errors")
                    }
                }
            }
        }
        stage('Build and Test') {  
            steps {  
                sh 'mvn clean test -Djacoco.diff.against=origin/master'  
            }  
        }
        stage('Analyze Coverage') {  
            steps {  
                recordCoverage(tools: [[parser: 'JACOCO']],
                    id: 'diff-check-jacoco', name: 'Diff-Check Jacoco Coverage',
                    sourceCodeRetention: 'EVERY_BUILD',
                    qualityGates: [
                            [threshold: 70.0, metric: 'LINE', baseline: 'PROJECT', unstable: true],
                            [threshold: 70.0, metric: 'BRANCH', baseline: 'PROJECT', unstable: true]])
            }  
        }   
    }
}
```
* Build pipeline and get the result
![diff-check-test-result](https://github.com/yangziwen/diff-check/assets/5212414/3a11da06-fc7e-4aad-845f-2f9d8d00f338)

### Others
* Besides [sun_checks.xml](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/sun_checks.xml) and [google_checks.xml](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml) provided by checkstyle by default, two other configurations [custom_checks.xml](https://github.com/yangziwen/diff-check/blob/master/diff-checkstyle/src/main/resources/custom_checks.xml) and [custom_full_checks.xml](https://github.com/yangziwen/diff-check/blob/master/diff-checkstyle/src/main/resources/custom_full_checks.xml) which compatible with the Alibaba code specification have been provided. You can use your favorite style configuration by specifying the absolute file path with <b>-c</b> option.
* Add the `lombok.config` file to the root folder of your project, and add a line of `lombok.addLombokGeneratedAnnotation = true` in the file, then all the methods generated by lombok will be ignored in the code coverage scanning([https://projectlombok.org/features/configuration](https://projectlombok.org/features/configuration)).
* Scanning with a changed file that has not been submitted and also not been added to the staging area may cause the modified code line calculated being inconsistent with the code line of the actual scanned file in the workspace, so please submit all changes first.

### Stargazers
[![Stargazers over time](https://starchart.cc/yangziwen/diff-check.svg?background=%23FFFFFF&axis=%23333333&line=%232f81f7)](https://starchart.cc/yangziwen/diff-check)

