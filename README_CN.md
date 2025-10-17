# diff-check
### 介绍
在使用检查工具扫描代码时，工具往往会一次性输出每个文件中的全部问题。这使得我们很难在清理完存量的问题之前，对正在开发的代码进行有效的增量检查。

针对这一痛点，本人对checkstyle、PMD、jacoco等工具进行了扩展，使其可基于Git的版本控制对增量代码中存在的问题进行检查并输出结果。

### 使用方法
#### 在本地提交commit时执行代码风格和缺陷检查
* 安装检查工具

	```Shell
	# 下载安装脚本
	curl https://raw.githubusercontent.com/yangziwen/diff-check/master/hooks/install.sh > install.sh
	
	# 安装钩子到指定的git代码库
	sh install.sh --repo-path=${the_absolute_path_of_your_git_repository}
	
	# 或者安装钩子到全局
	sh install.sh --global
	```

* 可通过以下方式开启或关闭检查

	```
	# 开启checkstyle检查
	git config diff-check.checkstyle.enabled true
	
	# 关闭checkstyle检查
	git config diff-check.checkstyle.enabled false
	
	# 开启PMD检查
	git config diff-check.pmd.enabled true
	
	# 关闭PMD检查
	git config diff-check.pmd.enabled false
	```

* 可通过以下方式设置检查规则

	```
	git config diff-check.checkstyle.config-file /custom_checks.xml
	
	git config diff-check.pmd.rulesets rulesets/java/ali-all.xml
	```

* 可通过以下方式设置代码库中的豁免路径

	```
	git config diff-check.checkstyle.exclude-regexp .+-client/.*
	
	git config diff-check.pmd.exclude-regexp .+-client/.*
	```

* 可通过以下方式设置输出结果的语言类型（当设置的语言不被支持时，英文en会被作为默认语言使用）

	```
	# 统一设置包括checkstyle和pmd在内的输出信息的语言类型
	git config diff-check.language en

	# 仅设置checkstyle输出信息的语言类型
	git config diff-check.checkstyle.language es

	# 仅设置pmd输出信息的语言类型
	git config diff-check.pmd.language zh
	```
 
* 可通过添加[`--global`](https://git-scm.com/docs/git-config)选项，使上述`git config`命令全局生效
#### 基于maven插件统计增量代码的单测覆盖率

* 配置maven插件

	```Xml
	<plugin>
	    <groupId>io.github.yangziwen</groupId>
	    <artifactId>diff-jacoco-maven-plugin</artifactId>
	    <version>0.0.6</version>
	    <configuration>
          <excludes>
            <!-- 指定需要排除的类路径 -->
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
	                <!-- 设置报告生成路径 -->
	                <outputDirectory>${project.reporting.outputDirectory}/jacoco-diff</outputDirectory>
	            </configuration>
	        </execution>
	    </executions>
	</plugin>
	```

* 执行测试和单测覆盖率统计

	```Shell
	# 通过为jacoco.diff.oldrev和jacoco.diff.newrev指定commit或分支实现增量覆盖率扫描
	# 如果不指定这两个变量，则进行全量覆盖率扫描
	mvn test -Djacoco.diff.oldrev=9ac9182 -Djacoco.diff.newrev=HEAD
	
	# 使用jacoco.diff.against参数，实现HEAD与任意其他commit或分支之间的merge-base的比对
	mvn test -Djacoco.diff.against=origin/master

	# 使用jacoco.author.name或jacoco.author.email参数，按指定提交者的增量代码，完成覆盖率扫描
	mvn test -Djacoco.diff.against=origin/master -Djacoco.author.name=yangziwen
	
	# 按如下方式可仅执行单个测试类，并生成报告
	mvn clean test -Djacoco.diff.against=origin/master -DskipTests=false -Dtest=io.github.yangziwen.quickdao.example.repository.UserSpringJdbcRepositoryTest -DfailIfNoTests=false
	```

* 效果

全量代码覆盖率 | 增量代码覆盖率
-|-
![全量覆盖率概览](https://github.com/user-attachments/assets/4d4d15db-81f0-4f1c-83b8-5cac751c4b19) | ![增量覆盖率概览](https://github.com/user-attachments/assets/c106fb48-0ead-4698-b23f-98cf599f1e75)
![全量覆盖率详情](https://github.com/user-attachments/assets/4954b7a3-7c4a-4c33-a0f6-bf9b5482fd4b) | ![增量覆盖率详情](https://github.com/user-attachments/assets/cf874bec-6400-448b-b7fd-33478c283ee8)

#### 将diff-check集成到Jenkins的流水线中
* 安装以下Jenkins插件
    * Pipeline Plugin
    * Git Plugin
    * Code Coverage API Plugin
    * Warning Next Generation Plugin
* 配置Jenkins流水线（以下内容仅用于效果展示）
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
* 执行构建并获得结果
![diff-check-test-result](https://github.com/yangziwen/diff-check/assets/5212414/3a11da06-fc7e-4aad-845f-2f9d8d00f338)


### 其他
在工程根目录添加**lombok.confg**文件，其中加入`lombok.addLombokGeneratedAnnotation = true`的配置，jacoco即可忽略所有lombok自动生成的方法([https://projectlombok.org/features/configuration](https://projectlombok.org/features/configuration))
