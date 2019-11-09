# diff-check
### 介绍
在使用检查工具扫描代码时，工具往往会一次性输出每个文件中的全部问题。这使得我们很难在清理完存量的问题之前，对正在开发的代码进行有效的增量检查。

针对这一痛点，本人对checkstyle、PMD、jacoco等工具进行了扩展，使其可基于Git的版本控制检查和输出增量变更的代码中存在的问题。

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
![全量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-summary.png) | ![增量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-summary.png)
![全量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-detail.png) | ![增量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-detail.png)

* 其他

    在工程根目录添加**lombok.confg**文件，其中加入`lombok.addLombokGeneratedAnnotation = true`的配置，jacoco即可忽略所有lombok自动生成的方法([https://projectlombok.org/features/configuration](https://projectlombok.org/features/configuration))
