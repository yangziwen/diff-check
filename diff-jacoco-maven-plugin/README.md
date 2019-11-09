# diff-jacoco-maven-plugin
### 介绍
基于Git的分支或commit之间的文件差异，扫描增量代码的单测覆盖率。

### 使用方法
#### 配置maven插件
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
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-diff</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```
#### 执行测试和单测覆盖率统计
```Shell
# 通过为jacoco.diff.oldrev和jacoco.diff.newrev指定commit或分支实现增量覆盖率扫描
# 如果不指定这两个变量，则进行全量覆盖率扫描
mvn test -Djacoco.diff.oldrev=9ac9182 -Djacoco.diff.newrev=HEAD

# 使用jacoco.diff.against参数，实现HEAD与任意其他commit或分支之间的merge-base的比对
mvn test -Djacoco.diff.against=origin/master

# 使用jacoco.author.name或jacoco.author.email参数，按指定提交者的增量代码，完成覆盖率扫描
mvn test -Djacoco.diff.against=origin/master -Djacoco.author.name=yangziwen
```

* 效果

全量代码覆盖率 | 增量代码覆盖率
-|-
![全量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-summary.png) | ![增量覆盖率概览](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-summary.png)
![全量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/full-coverage-detail.png) | ![增量覆盖率详情](https://raw.githubusercontent.com/wiki/yangziwen/diff-check/jacoco-images/incremental-coverage-detail.png)

#### 其他
  在工程根目录添加**lombok.confg**文件，其中加入`lombok.addLombokGeneratedAnnotation = true`的配置，jacoco即可忽略所有lombok自动生成的方法([https://projectlombok.org/features/configuration](https://projectlombok.org/features/configuration))
