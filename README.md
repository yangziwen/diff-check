# diff-check

[![Build Status](https://www.travis-ci.com/yangziwen/diff-check.svg?branch=master)](https://www.travis-ci.com/yangziwen/diff-check)

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
 
 You can run a single test class and get the report of it
 mvn clean test -Djacoco.diff.against=origin/master -DskipTests=false -Dtest=io.github.yangziwen.quickdao.example.repository.UserSpringJdbcRepositoryTest -DfailIfNoTests=false
```

* Get the result

Full Code Coverage | Incremental Code Coverage
-|-
![全量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-summary.png) | ![增量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-summary.png)
![全量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-detail.png) | ![增量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-detail.png)



### Others
* Besides [sun_checks.xml](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/sun_checks.xml) and [google_checks.xml](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml) provided by checkstyle by default, two other configurations [custom_checks.xml](https://github.com/yangziwen/diff-check/blob/master/diff-checkstyle/src/main/resources/custom_checks.xml) and [custom_full_checks.xml](https://github.com/yangziwen/diff-check/blob/master/diff-checkstyle/src/main/resources/custom_full_checks.xml) which compatible with the Alibaba code specification have been provided. You can use your favorite style configuration by specifying the absolute file path with <b>-c</b> option.
* Add the `lombok.config` file to the root folder of your project, and add a line of `lombok.addLombokGeneratedAnnotation = true` in the file, then all the methods generated by lombok will be ignored in the code coverage scanning([https://projectlombok.org/features/configuration](https://projectlombok.org/features/configuration)).
* Scanning with a changed file that has not been submitted and also not been added to the staging area may cause the modified code line calculated being inconsistent with the code line of the actual scanned file in the workspace, so please submit all changes first.
